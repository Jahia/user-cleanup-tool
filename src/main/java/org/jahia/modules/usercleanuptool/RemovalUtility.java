package org.jahia.modules.usercleanuptool;

import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
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
import java.util.function.Function;
import java.util.stream.Collectors;

public final class RemovalUtility {

    private static Logger logger = LoggerFactory.getLogger(RemovalUtility.class);

    public static final int SELECTION_SIZE = 25;
    public static final int QUERY_STEP = 30;

    public static void removeNode(String[] paths) throws RepositoryException {
        JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Void>() {
            @Override
            public Void doInJCR(JCRSessionWrapper jcrSessionWrapper) throws RepositoryException {

                for (String path : paths) {
                    if (jcrSessionWrapper.nodeExists(path)) {
                        jcrSessionWrapper.removeItem(path);
                        logger.info("Removed node: {}", path);
                    }
                }

                jcrSessionWrapper.save();
                return null;
            }
        });
    }

    public static List<User> getUsersFromAces(int offset) throws RepositoryException {
        String query = "select * from [jnt:ace]";
        Function<JCRNodeWrapper, Boolean> pred = node -> {
            try {
                if (node.hasProperty("j:principal") && node.getPropertyAsString("j:principal").startsWith("u:")) {
                    String userName = node.getPropertyAsString("j:principal").replace("u:", "");
                    JahiaUserManagerService um = JahiaUserManagerService.getInstance();
                    boolean existsGlobally = um.userExists(userName);
                    boolean existsLocally = um.userExists(userName, node.getResolveSite().getSiteKey());

                    return !existsGlobally && !existsLocally;
                }

                if (node.hasProperty("j:principal") && node.getPropertyAsString("j:principal").startsWith("g:")) {
                    String groupName = node.getPropertyAsString("j:principal").replace("g:", "");
                    JahiaGroupManagerService gm = JahiaGroupManagerService.getInstance();
                    boolean existsLocally = gm.groupExists(node.getResolveSite().getSiteKey(), groupName);

                    return !JahiaGroupManagerService.PROTECTED_GROUPS.contains(groupName) && !existsLocally && node.getPath().contains(String.format("/%s/", node.getResolveSite().getSiteKey()));
                }
            } catch (RepositoryException e) {
                logger.error("Failed to look up user", e);
            }

            return false;
        };

        return runQuery(query, pred, offset);
    }

    public static List<User> getMembers(int offset) throws RepositoryException {
        String query = "select * from [jnt:member] as m where m.['jcr:primaryType'] = 'jnt:member'";
        Function<JCRNodeWrapper, Boolean> pred = node -> {
            try {
                if (node.hasProperty("j:member")) {
                    String member = node.getPropertyAsString("j:member");
                    return node.getSession().getNodeByIdentifier(member) == null;
                }
            } catch (RepositoryException e) {
                return true;
            }

            return false;
        };

        return runQuery(query, pred, offset);
    }

    private static List<User> runQuery(String query, Function<JCRNodeWrapper, Boolean> predicate, int offset) throws RepositoryException {
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
}
