package com.adobe.cq.dialogupgrade.rules;

import com.adobe.cq.dialogupgrade.api.DialogRewriteRule;
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
public abstract class AbstractRewriteRule implements DialogRewriteRule {

    private Logger logger = LoggerFactory.getLogger(AbstractRewriteRule.class);

    private int ranking = Integer.MAX_VALUE;

    @Activate
    protected void activate(ComponentContext context)
            throws RepositoryException {
        @SuppressWarnings("unchecked")
        Dictionary<String, Object> props = context.getProperties();
        // read service ranking property
        Object ranking = props.get("service.ranking");
        if (ranking == null) {
            return;
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
