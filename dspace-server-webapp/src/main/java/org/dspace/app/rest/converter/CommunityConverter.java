/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.Community;
import org.dspace.content.service.CommunityService;
import org.dspace.discovery.IndexableObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the community in the DSpace API data model and
 * the REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class CommunityConverter
    extends DSpaceObjectConverter<Community, CommunityRest>
        implements IndexableObjectConverter<Community, CommunityRest> {

    @Autowired
    CommunityService communityService;

    public CommunityRest convert(Community community, Projection projection) {
        CommunityRest resource = super.convert(community, projection);

        resource.setArchivedItemsCount(
            communityService.countArchivedItems(ContextUtil.obtainCurrentRequestContext(), community));

        return resource;
    }

    @Override
    protected CommunityRest newInstance() {
        return new CommunityRest();
    }

    @Override
    public Class<Community> getModelClass() {
        return Community.class;
    }

    @Override
    public boolean supportsModel(IndexableObject idxo) {
        return idxo.getIndexedObject() instanceof Community;
    }
}
