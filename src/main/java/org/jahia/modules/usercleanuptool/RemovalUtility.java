package org.jahia.modules.usercleanuptool;

import org.jahia.registries.ServicesRegistry;
import org.jahia.services.cache.CacheHelper;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRStoreProvider;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.query.ScrollableQuery;
import org.jahia.services.usermanager.JahiaGroupManagerService;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class RemovalUtility {

    private static final Logger logger = LoggerFactory.getLogger(RemovalUtility.class);

    public static final int SELECTION_SIZE = 25;
    public static final int QUERY_STEP = 30;

    private static final String J_PRINCIPAL = "j:principal";
    private static final String DEFAULT_PROVIDER_KEY = "default";
    private static final String NT_ACE = "jnt:ace";
    private static final String NT_MEMBER = "jnt:member";

    private RemovalUtility() {
        // Utility class: hide the implicit public constructor.
    }

    public static void removeNode(String[] paths) throws RepositoryException {
        flushAllCaches();
        JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Void>() {
            @Override
            public Void doInJCR(JCRSessionWrapper jcrSessionWrapper) throws RepositoryException {

                for (String path : paths) {
                    if (jcrSessionWrapper.nodeExists(path)) {
                        JCRNodeWrapper node = jcrSessionWrapper.getNode(path);
                        if (isCleanableType(node)) {
                            jcrSessionWrapper.removeItem(path);
                            logger.info("Removed node: {}", path);
                        } else {
                            logger.warn("Skipped node not of a cleanable type ({} / {}): {}",
                                    NT_ACE, NT_MEMBER, path);
                        }
                    }
                }

                jcrSessionWrapper.save();
                return null;
            }
        });
    }

    /**
     * Defense-in-depth guard: this tool is only meant to clean up orphaned {@code jnt:ace} and
     * {@code jnt:member} nodes. Any other node type must never be removed, even if a path for it
     * is passed in.
     */
    static boolean isCleanableType(JCRNodeWrapper node) throws RepositoryException {
        if (node == null) {
            return false;
        }
        return node.isNodeType(NT_ACE) || node.isNodeType(NT_MEMBER);
    }

    public static List<User> getUsersFromAces(int offset) throws RepositoryException {
        flushAllCaches();
        String query = "select * from [jnt:ace]";
        Function<JCRNodeWrapper, Boolean> pred = node -> {
            try {
                if (node.hasProperty(J_PRINCIPAL) && node.getPropertyAsString(J_PRINCIPAL).startsWith("u:")) {
                    String userName = node.getPropertyAsString(J_PRINCIPAL).replace("u:", "");
                    JahiaUserManagerService um = JahiaUserManagerService.getInstance();
                    boolean existsGlobally = um.userExists(userName);
                    boolean existsLocally = um.userExists(userName, node.getResolveSite().getSiteKey());

                    return !existsGlobally && !existsLocally;
                }

                if (node.hasProperty(J_PRINCIPAL) && node.getPropertyAsString(J_PRINCIPAL).startsWith("g:")) {
                    String groupName = node.getPropertyAsString(J_PRINCIPAL).replace("g:", "");
                    JahiaGroupManagerService gm = JahiaGroupManagerService.getInstance();
                    boolean existsLocally = gm.groupExists(node.getResolveSite().getSiteKey(), groupName);

                    return !JahiaGroupManagerService.PROTECTED_GROUPS.contains(groupName) && !existsLocally && !gm.groupExists(null, groupName);
                }
            } catch (RepositoryException e) {
                logger.error("Failed to look up user", e);
            }

            return false;
        };

        return runQuery(query, pred, offset);
    }

    public static List<User> getMembers(int offset) throws RepositoryException {
        flushAllCaches();
        String query = "select * from [jnt:member] as m where m.['jcr:primaryType'] = 'jnt:member'";
        Function<JCRNodeWrapper, Boolean> pred = node -> {
            try {
                if (node.hasProperty("j:member")) {
                    String member = node.getPropertyAsString("j:member");
                    return node.getSession().getNodeByIdentifier(member) == null;
                } else {
                	return true; //jnt:member must have a j:member else it is invalid
                }
            } catch (RepositoryException e) {
                return true;  //in case of error return true
            }
        };

        return runQuery(query, pred, offset);
    }

    private static List<User> runQuery(String query, Function<JCRNodeWrapper, Boolean> predicate, int offset) throws RepositoryException {
        flushAllCaches();
        return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<List<User>>() {
            @Override
            public List<User> doInJCR(JCRSessionWrapper jcrSessionWrapper) throws RepositoryException {
                List<JCRNodeWrapper> list = new ArrayList<>();
                QueryManager qm = jcrSessionWrapper.getWorkspace().getQueryManager();
                Query q = qm.createQuery(query, Query.JCR_SQL2);
                ScrollableQuery scrollableQuery = new ScrollableQuery(QUERY_STEP, q);
                scrollableQuery.execute(new Scroller(predicate, list, SELECTION_SIZE, offset));

                return list.stream().map(n -> {
                    try {
                        return new User(n.getName(), n.getPath(), n.getPrimaryNodeTypeName());
                    } catch (RepositoryException e) {
                        logger.error("Failed to get node info", e);
                    }
                    return null;
                }).collect(Collectors.toList());
            }
        });

    }

    private static void flushAllCaches() {
        //flush user/group caches to get the correct results
        CacheHelper.flushEhcacheByName("LDAPUsersCache", true);
        CacheHelper.flushEhcacheByName("LDAPGroupCache", true);
        CacheHelper.flushEhcacheByName("org.jahia.services.usermanager.JahiaGroupManagerService.membershipCache", true);
        CacheHelper.flushEhcacheByName("org.jahia.services.usermanager.JahiaUserManagerService.userPathByUserNameCache", true);
        CacheHelper.flushEhcacheByName("org.jahia.services.usermanager.JahiaGroupManagerService.groupPathByGroupNameCache", true);

    }

    public static List<JCRStoreProvider> getExternalUserProvider() throws RepositoryException {
        JahiaUserManagerService userManager = JahiaUserManagerService.getInstance();
        return collectExternalProviders(
                userManager::getProviderList,
                userManager::getProviderList);
    }

    public static List<JCRStoreProvider> getExternalGroupProvider() throws RepositoryException {
        JahiaGroupManagerService groupManager = JahiaGroupManagerService.getInstance();
        return collectExternalProviders(
                session -> groupManager.getProviderList(null, session),
                groupManager::getProviderList);
    }

    /**
     * Shared logic for {@link #getExternalUserProvider()} and {@link #getExternalGroupProvider()}:
     * collect the global providers, then the per-site providers, filtering out the built-in
     * {@code default} store provider.
     *
     * @param globalProviders fetches the global provider list for the given session
     * @param siteProviders   fetches the provider list for a given site key and session
     */
    private static List<JCRStoreProvider> collectExternalProviders(
            Function<JCRSessionWrapper, List<JCRStoreProvider>> globalProviders,
            BiFunction<String, JCRSessionWrapper, List<JCRStoreProvider>> siteProviders)
            throws RepositoryException {

        List<JCRStoreProvider> providers = new ArrayList<>();
        JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession();

        addExternalProviders(providers, globalProviders.apply(session));

        List<String> sites = ServicesRegistry.getInstance().getJahiaSitesService().getSitesNames();
        for (String site : sites) {
            addExternalProviders(providers, siteProviders.apply(site, session));
        }
        return providers;
    }

    private static void addExternalProviders(List<JCRStoreProvider> target, List<JCRStoreProvider> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (JCRStoreProvider prov : source) {
            if (!DEFAULT_PROVIDER_KEY.equals(prov.getKey())) {
                target.add(prov);
            }
        }
    }
}
