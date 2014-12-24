package com.adobe.cq.dialogupgrade;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Dictionary;

/**
 * Abstract dialog upgrade rule implementing {@link DialogRewriteRule#getRanking}.
 */
@Component(componentAbstract = true)
public abstract class AbstractDialogRewriteRule implements DialogRewriteRule {

    private Logger logger = LoggerFactory.getLogger(AbstractDialogRewriteRule.class);

    private int ranking = Integer.MAX_VALUE;

    @Activate
    protected void activate(ComponentContext context)
            throws RepositoryException {
        @SuppressWarnings("unchecked")
        Dictionary<String, Object> props = context.getProperties();
        // read service ranking property
        Object ranking = props.get("service.ranking");
        if (ranking == null) {
            ranking = -1;
        }
        this.ranking = (Integer) ranking;
    }

    public int getRanking() {
        return ranking;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[ranking=" + getRanking() + "]";
    }
}
