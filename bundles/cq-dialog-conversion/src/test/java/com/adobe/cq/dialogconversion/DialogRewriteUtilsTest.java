/*************************************************************************
 *
 * ADOBE CONFIDENTIAL
 * __________________
 *
 *  Copyright 2017 Adobe Systems Incorporated
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 **************************************************************************/

package com.adobe.cq.dialogconversion;

import io.wcm.testing.mock.aem.junit.AemContext;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.jcr.Node;

import static org.junit.Assert.*;

public class DialogRewriteUtilsTest {

    private static final String DIALOGS_ROOT = "/libs/cq/dialogconversion/content/dialogs";

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @Before
    public void setUp() throws Exception {
        context.load().json("/test-dialogs.json", DIALOGS_ROOT);
    }

    @Test
    public void testGetDialogTypeClassic() throws Exception {
        boolean allClassic = false;

        String[] classicDialogPaths = {
            DIALOGS_ROOT + "/classic/dialog",
            DIALOGS_ROOT + "/classic/design_dialog"
        };

        for (String path: classicDialogPaths) {
            Resource resource = context.resourceResolver().getResource(path);
            Node node = resource.adaptTo(Node.class);
            DialogType type = DialogRewriteUtils.getDialogType(node);
            allClassic = (type == DialogType.CLASSIC);
            if (!allClassic) {
                break;
            }
        }

        assertTrue(allClassic);
    }

    @Test
    public void testGetDialogTypeCoral2() throws Exception {
        boolean allCoral2 = false;

        String[] coral2DialogPaths = {
            DIALOGS_ROOT + "/coral2/cq:dialog",
            DIALOGS_ROOT + "/coral2/cq:design_dialog",
            DIALOGS_ROOT + "/backupsnoreplacementdiscarded/cq:dialog.coral2",
            DIALOGS_ROOT + "/backupsnoreplacementdiscarded/cq:design_dialog.coral2",
            DIALOGS_ROOT + "/level1/classicandcoral2/cq:dialog",
            DIALOGS_ROOT + "/level1/classicandcoral2/cq:design_dialog"
        };

        for (String path: coral2DialogPaths) {
            Resource resource = context.resourceResolver().getResource(path);
            Node node = resource.adaptTo(Node.class);
            DialogType type = DialogRewriteUtils.getDialogType(node);
            allCoral2 = (type == DialogType.CORAL_2);
            if (!allCoral2) {
                break;
            }
        }

        assertTrue(allCoral2);
    }

    @Test
    public void testGetDialogTypeCoral3() throws Exception {
        boolean allCoral3 = false;

        String[] coral3DialogPaths = {
            DIALOGS_ROOT + "/level1/converted/cq:dialog",
            DIALOGS_ROOT + "/level1/converted/cq:design_dialog"
        };

        for (String path: coral3DialogPaths) {
            Resource resource = context.resourceResolver().getResource(path);
            Node node = resource.adaptTo(Node.class);
            DialogType type = DialogRewriteUtils.getDialogType(node);
            allCoral3 = (type == DialogType.CORAL_3);
            if (!allCoral3) {
                break;
            }
        }

        assertTrue(allCoral3);
    }

    @Test
    public void testIsDesignDialog() throws Exception {
        boolean allDesignDialogs = false;

        String[] designDialogPaths = {
            DIALOGS_ROOT + "/classic/design_dialog",
            DIALOGS_ROOT + "/coral2/cq:design_dialog",
            DIALOGS_ROOT + "/backupsnoreplacementdiscarded/cq:design_dialog.coral2",
            DIALOGS_ROOT + "/level1/classicandcoral2/design_dialog",
            DIALOGS_ROOT + "/level1/classicandcoral2/cq:design_dialog"
        };

        for (String path: designDialogPaths) {
            Resource resource = context.resourceResolver().getResource(path);
            Node node = resource.adaptTo(Node.class);
            allDesignDialogs = DialogRewriteUtils.isDesignDialog(node);
            if (!allDesignDialogs) {
                break;
            }
        }

        assertTrue(allDesignDialogs);
    }
}