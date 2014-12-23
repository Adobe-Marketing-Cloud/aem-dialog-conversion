package com.adobe.cq.dialogupgrade;

import com.adobe.cq.dialogupgrade.treerewriter.RewriteException;

/**
 * Thrown when the rewrite operation of a dialog fails.
 */
public class DialogRewriteException extends RewriteException {

    public DialogRewriteException(String message) {
        super(message);
    }

    public DialogRewriteException(String message, Throwable cause) {
        super(message, cause);
    }

}
