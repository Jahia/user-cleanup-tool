package org.jahia.modules.userremovaltool;

import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.query.ScrollableQueryCallback;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.util.List;
import java.util.function.Function;

public class Scroller extends ScrollableQueryCallback<Void> {
    private Function<JCRNodeWrapper, Boolean> predicate;
    private List<JCRNodeWrapper> tray;
    private int capacity;
    private int offset;

    public Scroller(Function<JCRNodeWrapper, Boolean> predicate, List<JCRNodeWrapper> tray, int capacity, int offset) {
        this.predicate = predicate;
        this.tray = tray;
        this.capacity = capacity;
        this.offset = offset;
    }

    @Override
    public boolean scroll() throws RepositoryException {
        NodeIterator nodeIterator = stepResult.getNodes();

        while(nodeIterator.hasNext() && tray.size() < capacity) {
            JCRNodeWrapper node = (JCRNodeWrapper) nodeIterator.nextNode();
            if (Boolean.TRUE.equals(predicate.apply(node))) {
                if (offset > 0) {
                    offset--;
                } else {
                    tray.add(node);
                }
            }
        }

        return tray.size() < capacity;
    }

    @Override
    protected Void getResult() {
        return null;
    }
}
