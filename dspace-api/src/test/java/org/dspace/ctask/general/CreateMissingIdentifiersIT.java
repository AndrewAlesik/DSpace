/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.curate.Curator;
import org.dspace.identifier.IdentifierProvider;
import org.dspace.identifier.IdentifierServiceImpl;
import org.dspace.identifier.VersionedHandleIdentifierProviderWithCanonicalHandles;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Test;

/**
 * Rudimentary test of the curation task.
 *
 * @author mwood
 */
public class CreateMissingIdentifiersIT
        extends AbstractIntegrationTestWithDatabase {
    private ServiceManager serviceManager;
    private IdentifierServiceImpl identifierService;
    private static final String P_TASK_DEF
            = "plugin.named.org.dspace.curate.CurationTask";
    private static final String TASK_NAME = "test";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        serviceManager = DSpaceServicesFactory.getInstance().getServiceManager();
        identifierService = serviceManager.getServicesByType(IdentifierServiceImpl.class).get(0);
        // Clean out providers to avoid any being used for creation of community and collection
        identifierService.setProviders(new ArrayList<>());
    }

    @Test
    public void testPerform()
            throws IOException {
        // Must remove any cached named plugins before creating a new one
        CoreServiceFactory.getInstance().getPluginService().clearNamedPluginClasses();
        ConfigurationService configurationService = kernelImpl.getConfigurationService();
        // Define a new task dynamically
        configurationService.setProperty(P_TASK_DEF,
                CreateMissingIdentifiers.class.getCanonicalName() + " = " + TASK_NAME);

        Curator curator = new Curator();
        curator.addTask(TASK_NAME);

        context.setCurrentUser(admin);
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .build();
        Item item = ItemBuilder.createItem(context, collection)
                               .build();

        /*
         * Curate with regular test configuration -- should succeed.
         */
        curator.curate(context, item);
        int status = curator.getStatus(TASK_NAME);
        assertEquals("Curation should succeed", Curator.CURATE_SUCCESS, status);

        /*
         * Now install an incompatible provider to make the task fail.
         */
        registerProvider(VersionedHandleIdentifierProviderWithCanonicalHandles.class);

        curator.curate(context, item);
        System.out.format("With incompatible provider, result is '%s'.\n",
                curator.getResult(TASK_NAME));
        assertEquals("Curation should fail", Curator.CURATE_ERROR,
                curator.getStatus(TASK_NAME));
    }

    @Override
    @After
    public void destroy() throws Exception {
        super.destroy();
        DSpaceServicesFactory.getInstance().getServiceManager().getApplicationContext().refresh();
    }

    private void registerProvider(Class type) {
        // Register our new provider
        serviceManager.registerServiceClass(type.getName(), type);
        IdentifierProvider identifierProvider =
                (IdentifierProvider) serviceManager.getServiceByName(type.getName(), type);

        // Overwrite the identifier-service's providers with the new one to ensure only this provider is used
        identifierService.setProviders(List.of(identifierProvider));
    }
}
