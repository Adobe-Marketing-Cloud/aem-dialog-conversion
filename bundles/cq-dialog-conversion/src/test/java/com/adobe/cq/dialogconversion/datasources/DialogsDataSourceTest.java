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

package com.adobe.cq.dialogconversion.datasources;

import io.wcm.testing.mock.aem.junit.AemContext;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ExpressionResolver;
import com.day.cq.commons.Externalizer;
import com.day.cq.commons.impl.CommonAdapterFactory;

import org.apache.commons.collections.IteratorUtils;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.testing.jcr.RepositoryUtil;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.ResourceResolverType;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@RunWith(MockitoJUnitRunner.class)
public class DialogsDataSourceTest {

    private static final String DIALOGS_ROOT = "/libs/cq/dialogconversion/content/dialogs";
    private static final String ITEM_RESOURCE_TYPE = "cq/dialogconversion/components/dialogs/item";

    private DialogsDataSource dialogsDataSource;

    @Rule
    public AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingHttpServletResponse response;

    @Mock
    private Resource requestResource;

    @Mock
    private RequestPathInfo requestPathInfo;

    @Mock
    private ExpressionResolver expressionResolver;

    @Before
    public void setUp() throws IOException, RepositoryException, ParseException {
        ResourceResolver resolver = context.resourceResolver();

        Session adminSession = resolver.adaptTo(Session.class);
        RepositoryUtil.registerNodeType(adminSession, getClass().getResourceAsStream("/nodetypes/nodetypes.cnd"));

        context.load().json("/test-dialogs.json", DIALOGS_ROOT);

        // register mock services
        context.registerService(Externalizer.class, Mockito.mock(Externalizer.class));

        // register adapter factories
        CommonAdapterFactory adapterFactory = (CommonAdapterFactory) context.registerService(AdapterFactory.class, new CommonAdapterFactory());
        MockOsgi.injectServices(adapterFactory, context.bundleContext());

        // register data source
        dialogsDataSource = context.registerService(DialogsDataSource.class, new DialogsDataSource());

        // prepare request resource
        HashMap<String, Object> properties = new HashMap<String, Object>();
        properties.put("path", DIALOGS_ROOT);
        properties.put("itemResourceType", ITEM_RESOURCE_TYPE);
        Mockito.stub(requestResource.getValueMap()).toReturn(new ValueMapDecorator(properties));

        // mock request
        Mockito.stub(request.getLocale()).toReturn(Locale.US);
        Mockito.stub(request.getResource()).toReturn(requestResource);
        Mockito.stub(request.getResourceResolver()).toReturn(resolver);
        Mockito.stub(request.getRequestPathInfo()).toReturn(requestPathInfo);

        // prepare expression resolver
        Mockito.stub(expressionResolver.resolve(DIALOGS_ROOT, Locale.US, String.class, request)).toReturn(DIALOGS_ROOT);
        Whitebox.setInternalState(dialogsDataSource, "expressionResolver", expressionResolver);
    }

    @Test
    public void testDoGet() throws Exception {
        List<String> expectedDialogPaths = new ArrayList<String>();
        expectedDialogPaths.addAll(Arrays.asList(
            DIALOGS_ROOT + "/classic/dialog",
            DIALOGS_ROOT + "/classic/design_dialog",
            DIALOGS_ROOT + "/coral2/cq:dialog",
            DIALOGS_ROOT + "/coral2/cq:design_dialog",
            DIALOGS_ROOT + "/level1/classicandcoral2/cq:dialog",
            DIALOGS_ROOT + "/level1/classicandcoral2/cq:design_dialog",
            DIALOGS_ROOT + "/level1/converted/dialog",
            DIALOGS_ROOT + "/level1/converted/cq:design_dialog.coral2",
            DIALOGS_ROOT + "/level1/coral2andbackup/cq:dialog"));

        dialogsDataSource.doGet(request, response);

        ArgumentCaptor<DataSource> dataSourceArgumentCaptor = ArgumentCaptor.forClass(DataSource.class);
        Mockito.verify(request).setAttribute(Mockito.anyString(), dataSourceArgumentCaptor.capture());

        DataSource dialogsDataSource = dataSourceArgumentCaptor.getValue();

        @SuppressWarnings("unchecked")
        List<Resource> dialogsList = IteratorUtils.toList(dialogsDataSource.iterator());

        assertEquals(expectedDialogPaths.size(), dialogsList.size());

        for (Resource dialogResource : dialogsList) {
            ValueMap valueMap = dialogResource.getValueMap();

            // expected properties
            assertNotNull(valueMap.get("dialogPath"));
            assertNotNull(valueMap.get("type"));
            assertNotNull(valueMap.get("href"));
            assertNotNull(valueMap.get("converted"));
            assertNotNull(valueMap.get("crxHref"));

            expectedDialogPaths.remove(valueMap.get("dialogPath"));
        }

        assertEquals(0, expectedDialogPaths.size());
    }
}
