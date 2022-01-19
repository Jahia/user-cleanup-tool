package org.jahia.modules.userremovaltool;

import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.query.ScrollableQuery;
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

    private static final int SIZE = 10;

    public static void removeNode(String[] paths) {
        System.out.println(paths);
    }

    public static List<User> getUsersFromAces() throws RepositoryException {
        String query = "select * from [jnt:ace]";
        Function<JCRNodeWrapper, Boolean> pred = node -> {
            try {
                if (node.hasProperty("j:principal") && node.getPropertyAsString("j:principal").startsWith("u:")) {
                    String userName = node.getPropertyAsString("j:principal").replace("u:", "");
                    return !JahiaUserManagerService.getInstance().userExists(userName);
                }
            } catch (RepositoryException e) {
                logger.error("Failed to look up user", e);
            }

            return false;
        };

        return runQuery(query, pred, 0);
    }

    public static List<User> getMembers() throws RepositoryException {
        String query = "select * from [jnt:member] as m where m.['jcr:primaryType'] = 'jnt:member'";
        Function<JCRNodeWrapper, Boolean> pred = node -> {
            try {
                if (node.hasProperty("j:member")) {
                    String member = node.getPropertyAsString("j:member");
                    return node.getSession().getNodeByUUID(member) == null;
                }
            } catch (RepositoryException e) {
                return true;
            }

            return false;
        };

        return runQuery(query, pred, 0);
    }

    private static List<User> runQuery(String query, Function<JCRNodeWrapper, Boolean> predicate, int offset) throws RepositoryException {
        return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<List<User>>() {
            @Override
            public List<User> doInJCR(JCRSessionWrapper jcrSessionWrapper) throws RepositoryException {
                List<JCRNodeWrapper> list = new ArrayList<>();
                QueryManager qm = jcrSessionWrapper.getWorkspace().getQueryManager();
                Query q = qm.createQuery(query, Query.JCR_SQL2);
                ScrollableQuery scrollableQuery = new ScrollableQuery(SIZE, q);
                scrollableQuery.execute(new Scroller(predicate, list, 20, offset));

                return list.stream().map(n -> {
                    try {
                        return new User(n.getName(), n.getPath(), n.getPrimaryNodeTypeName());
                    } catch (RepositoryException e) {
                        System.out.println(" *********** Error");
                    }
                    return null;
                }).collect(Collectors.toList());
            }
        });

    }
}
