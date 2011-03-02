package org.hippoecm.frontend.plugins.gallery.editor.crop;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.hippoecm.frontend.plugins.yui.AbstractYuiBehavior;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.onehippo.yui.YahooNamespace;

/**
 * Created by IntelliJ IDEA. User: mchatzidakis Date: 2/28/11 Time: 3:07 PM To change this template use File | Settings
 * | File Templates.
 */
public class CropBehavior extends AbstractYuiBehavior {

    private String regionInputId;
    private String imagePreviewContainerId;
    private Dimension originalImageDimension;
    private Dimension thumbnailDimension;


    public CropBehavior(String regionInputId, String imagePreviewContainerId, Dimension originalImageDimension, Dimension thumbnailDimension){
        this.regionInputId = regionInputId;
        this.imagePreviewContainerId = imagePreviewContainerId;
        this.originalImageDimension = originalImageDimension;
        this.thumbnailDimension = thumbnailDimension;
    }

    @Override
    public void bind(final Component component) {
        super.bind(component);
        component.setOutputMarkupId(true);
    }

    @Override
    public void addHeaderContribution(IYuiContext context)  {
        context.addModule(YahooNamespace.NS, "imagecropper");
        context.addOnDomLoad(new AbstractReadOnlyModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getObject() {
                return getInitString();
            }
        });
        context.addCssReference(new ResourceReference(YahooNamespace.class, YahooNamespace.NS.getPath()+"imagecropper/assets/skins/sam/imagecropper-skin.css"));
        context.addCssReference(new ResourceReference(YahooNamespace.class, YahooNamespace.NS.getPath()+"resize/assets/skins/sam/resize-skin.css"));
    }


    private String getInitString() {
        PackagedTextTemplate cropperJsTemplate = new PackagedTextTemplate(CropBehavior.class, "Hippo.ImageCropper.js");
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("originalImageMarkupId", getComponent().getMarkupId());
        parameters.put("imagePreviewContainerMarkupId", imagePreviewContainerId);
        parameters.put("regionInputMarkupId", regionInputId);
        parameters.put("originalImageWidth", originalImageDimension.getWidth());
        parameters.put("originalImageHeight", originalImageDimension.getHeight());
        parameters.put("thumbnailWidth", thumbnailDimension.getWidth());
        parameters.put("thumbnailHeight", thumbnailDimension.getHeight());

        return cropperJsTemplate.interpolate(parameters).getString();
    }


}

