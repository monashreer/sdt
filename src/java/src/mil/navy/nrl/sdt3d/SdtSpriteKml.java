package mil.navy.nrl.sdt3d;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.swing.JMenuItem;
import javax.xml.stream.XMLStreamException;

import gov.nasa.worldwind.WorldWind;
/*!
 * Known KML limitations:
 * 
 *  1. Images (icons) can not be associated with placemarks
 *  2. Have been having trouble getting hrefs to work correctly...
 *  
 *  /
 */
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.ogc.collada.ColladaRoot;
import gov.nasa.worldwind.ogc.kml.KMLAbstractContainer;
import gov.nasa.worldwind.ogc.kml.KMLAbstractFeature;
import gov.nasa.worldwind.ogc.kml.KMLLookAt;
import gov.nasa.worldwind.ogc.kml.KMLPlacemark;
import gov.nasa.worldwind.ogc.kml.KMLRoot;
import gov.nasa.worldwind.ogc.kml.impl.KMLController;
import gov.nasa.worldwind.ogc.kml.impl.KMLModelPlacemarkImpl;
import gov.nasa.worldwind.ogc.kml.impl.KMLRenderable;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.util.layertree.KMLLayerTreeNode;

public class SdtSpriteKml extends SdtSpriteModel 
{

	KMLController kmlController = null;

	KMLRoot kmlRoot = null;

	ColladaRoot colladaRoot = null;
	
	String fileName = null;
	
	// For kml not associated with a node

	File kmlFile = null;

	KMLLookAt lookAt = null;

	JMenuItem kmlMenuItem = null;

	KMLLayerTreeNode layerNode = null;

	public SdtSpriteKml(SdtSpriteKml template)
	{
		super(template);
		this.fileName = template.fileName;
		this.kmlFile = template.kmlFile;
	}


	public SdtSpriteKml(String name)
	{
		super(name);
	}

	public SdtSpriteKml(SdtSpriteIcon template) 
	{
		super(template);
	}

	
	public SdtSpriteKml(SdtSprite template) 
	{
		super(template);
	}


	@Override
	boolean isValid()
	{
		return colladaRoot != null;
	}

	public KMLRoot initializeKmlRoot()
	{
		KMLRoot kmlRoot = null;
		try
		{
			kmlRoot = KMLRoot.createAndParse(fileName);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (XMLStreamException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return kmlRoot;
	}

	
	@Override
	public ColladaRoot getColladaRoot()
	{
		return colladaRoot;
	}
	
	
	
	ColladaRoot getColladaRootFromPlacemark(KMLPlacemark feature)
	{
		List<KMLRenderable> rs = ((KMLPlacemark) feature).getRenderables();
		if (rs != null)
		{
			for (KMLRenderable r : rs)
			{
				if (r instanceof KMLModelPlacemarkImpl)
				{
					if (((KMLModelPlacemarkImpl) r).getColladaRoot() != null)
					{
						colladaRoot = ((KMLModelPlacemarkImpl) r).getColladaRoot();
						if (colladaRoot != null)
						{
							colladaRoot.setModelScale(this.getModelScale());
							// The kml renderer does it's own terrain position computations
							// so set the root to absolute and use our calcs from
							// the node render pass
							colladaRoot.setAltitudeMode(WorldWind.ABSOLUTE);

						}
					}
				}
			}
		}
		return colladaRoot;
	}
	
	
	/*
	 * Kml collada roots cannot be shared as 3d model meshs can.
	 */
	public ColladaRoot getColladaRootFromKmlRoot(KMLRoot kmlRoot)
	{
		if (colladaRoot != null)
			return colladaRoot;
		
		KMLAbstractFeature feature = kmlRoot.getFeature();
		
		if (feature instanceof KMLPlacemark)
		{
			colladaRoot = getColladaRootFromPlacemark((KMLPlacemark)feature);
		}
		else if (feature instanceof KMLAbstractContainer)
		{
			for (KMLAbstractFeature abstractFeature : ((KMLAbstractContainer) feature).getFeatures())
			{
				if (abstractFeature instanceof KMLPlacemark)
				{
					colladaRoot = getColladaRootFromPlacemark((KMLPlacemark) abstractFeature);
				}
			}
		}
		
		/*
		if (kmlRoot != null && kmlRoot.getFeature() != null)
		{
			KMLAbstractFeature kmlAbstractFeature = kmlRoot.getFeature();
			for (KMLAbstractFeature feature : ((KMLAbstractContainer) kmlAbstractFeature).getFeatures())
			{
				if (feature instanceof KMLPlacemark)
				{
					List<KMLRenderable> rs = ((KMLPlacemark) feature).getRenderables();
					if (rs != null)
					{
						for (KMLRenderable r : rs)
						{
							if (r instanceof KMLModelPlacemarkImpl)
							{
								if (((KMLModelPlacemarkImpl) r).getColladaRoot() != null)
								{
									colladaRoot = ((KMLModelPlacemarkImpl) r).getColladaRoot();
									if (colladaRoot != null)
									{
										colladaRoot.setModelScale(this.getModelScale());
										// The kml renderer does it's own terrain position computations
										// so set the root to absolute and use our calcs from
										// the node render pass
										colladaRoot.setAltitudeMode(WorldWind.ABSOLUTE);

									}
								}
							}
						}
					}
				}
			}
		} // end if feature != null
		*/
		return colladaRoot;
	}


	public double getPitch()
	{
		if (modelPitch != 999.0)
		{
			return modelPitch;
		}
		return 0.0;
	}


	@Override
	public double getYaw()
	{
		// Sprites are offset by 180 degrees
		if (this.modelYaw != 999.0)
		{
			return this.modelYaw + 180.0;
		}
		return 180.0;
	}


	public double getRoll()
	{
		if (modelRoll != 999.0)
		{
			return modelRoll;
		}
		return 0.0;
	}


	// Collada root requires model scale in Vec4
	public Vec4 getModelScale()
	{
		Double x = (double) scale;
		Double y = x;
		Double z = x;
		Vec4 modelScale = new Vec4(
			x != null ? x : 1.0,
			y != null ? y : 1.0,
			z != null ? z : 1.0);

		return modelScale;

	}


	public String getFileName()
	{
		return fileName;
	}


	public void setKmlFilename(String fileName)
	{
		this.fileName = fileName;
	}
	

	@Override
	public void setFixedLength(double length)
	{
		fixedLength = length;
	}

	
	/*
	 * Called by rendering function
	 */	
	public Vec4 computeSizeVector(DrawContext dc, Vec4 loc)
	{		
		Vec4 modelScaleVec;
		if (getFixedLength() > 0.0 && isRealSize)
		{
			// if "real-world" size use fixed length
			double localSize = getFixedLength();
			Double scale = (double) getScale();
			modelScaleVec = new Vec4(localSize * scale, localSize * scale, localSize * scale);
		}
		else
		{			
			double d = loc.distanceTo3(dc.getView().getEyePoint());
			double pSize = dc.getView().computePixelSizeAtDistance(d);			

			// First see if psize is less than our fixed length
			double fixedLength = getFixedLength();
			double width = (iconWidth > iconHeight) ? iconWidth : iconHeight;
			if (fixedLength < 0.0 && width > 0) fixedLength = iconWidth;
			
			pSize = pSize * fixedLength;
			if (pSize < fixedLength)
			{
				pSize = fixedLength;
			}
			else
			{
				// If not calculate psize for iconWidth
				d = loc.distanceTo3(dc.getView().getEyePoint());
				pSize = dc.getView().computePixelSizeAtDistance(d);			
				width = (iconWidth > iconHeight) ? iconWidth : iconHeight;
				pSize = pSize * width;
				if (pSize < width)
					pSize = width;
			}

			// Finally scale the model 
			
			// TODO: scale is not working properly - models get turned upside down
			// and scale size behaves erractically (when same kml is loaded
			// multiple times??)
			Double scale = (double) getScale();
			modelScaleVec = new Vec4(pSize * scale, pSize * scale, pSize * scale);
		}
		return modelScaleVec;
	}


	public void setLayerNode(KMLLayerTreeNode theLayerNode)
	{
		layerNode = theLayerNode;
	}


	public KMLLayerTreeNode getLayerNode()
	{
		return layerNode;
	}


	public boolean hasController()
	{
		return kmlController != null;
	}


	public String getMenuName()
	{
		// If we've loaded the kml from the menu, set the menu item to the file name only
		// otherwise we have a user defined kml name..
		if (getName().contains(System.getProperty("file.separator")))
		{
			return getKmlFile().getName();
		}
		return getName();
	}

	
	public File getKmlFile()
	{
		return kmlFile;
	}


	public KMLLookAt getLookAt()
	{
		return lookAt;
	}


	public JMenuItem getKmlMenuItem()
	{
		return kmlMenuItem;
	}


	public boolean setKmlFile(Object fileName)
	{
		// Used when kml is not associated with a node
		try
		{
			kmlRoot = KMLRoot.createAndParse(fileName);
			if (kmlRoot == null)
			{
				System.out.println("Unable to parse kml file " + fileName);
				return false;
			}
			kmlRoot.setField(AVKey.DISPLAY_NAME, formName(kmlFile, kmlRoot));

			KMLAbstractFeature kmlFeature = kmlRoot.getFeature();
			lookAt = (KMLLookAt) kmlFeature.getField("AbstractView");
			kmlController = new KMLController(kmlRoot);
			// Keep a reference to the menu items so we can delete them
			kmlMenuItem = new JMenuItem(getName());

			return true;

		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (XMLStreamException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}


	protected static String formName(Object kmlSource, KMLRoot kmlRoot)
	{
		KMLAbstractFeature rootFeature = kmlRoot.getFeature();

		if (rootFeature != null && !WWUtil.isEmpty(rootFeature.getName()))
			return rootFeature.getName();

		if (kmlSource instanceof File)
			return ((File) kmlSource).getName();

		if (kmlSource instanceof URL)
			return ((URL) kmlSource).getPath();

		if (kmlSource instanceof String && WWIO.makeURL((String) kmlSource) != null)
			return WWIO.makeURL((String) kmlSource).getPath();

		return "KML Layer ";
	}


	// Used by "fixed" kml objects
	@Override
	public KMLController getKmlController()
	{
		if (kmlRoot == null)
			kmlRoot = initializeKmlRoot();

		if (kmlRoot != null && kmlController == null)
			this.kmlController = new KMLController(kmlRoot);

		return kmlController;
	}


	// Used by "fixed" kml objects
	public KMLRoot getKmlRoot()
	{
		return kmlRoot;
	}

	
	@Override
	public void render(DrawContext dc) 
	{
		
		if (kmlController == null)
		{
			getKmlController();
		}
		
		if (colladaRoot == null)
			colladaRoot = getColladaRootFromKmlRoot(kmlController.getKmlRoot());

		if (colladaRoot == null)
		{
			// don't recreate collada root each render pass! fix!
			return;
		}
		
		Vec4 loc = dc.getGlobe().computePointFromPosition(getPosition());

		// Set model position & size
		colladaRoot.setPosition(getPosition());
		Vec4 modelScaleVector = this.computeSizeVector(dc, loc);
		colladaRoot.setModelScale(modelScaleVector);

		// Set model orientation
		colladaRoot.setHeading(Angle.fromDegrees(heading));;
		// kml roll is the reverse of models (and our default)
		colladaRoot.setRoll(Angle.fromDegrees(-(roll + getModelRoll())));
		colladaRoot.setPitch(Angle.fromDegrees(pitch + getModelPitch()));
		
		Vec4 modelPoint = null;
		if (getPosition().getElevation() < dc.getGlobe().getMaxElevation())
			modelPoint = dc.getSurfaceGeometry().getSurfacePoint(getPosition());
		if (modelPoint == null)
			modelPoint = dc.getGlobe().computePointFromPosition(getPosition());

		Vec4 screenPoint = dc.getView().project(modelPoint);
		Vec4 modelScale = colladaRoot.getModelScale();
		Rectangle rect = new Rectangle((int) (screenPoint.x), (int) (screenPoint.y),
			(int) (modelScale.x), (int) (modelScale.y));

		this.recordFeedback(dc, kmlController, modelPoint, rect);
		kmlController.render(dc);


	}
	// These are duplicate functions from the icon renderer
	/**
	 * Returns true if the ModelRenderer should record feedback about how the specified kmlModel has been processed.
	 *
	 * @param dc the current DrawContext.
	 * @param model the KMLModel to record feedback information for.
	 *
	 * @return true to record feedback; false otherwise.
	 */
	protected boolean isFeedbackEnabled(DrawContext dc, KMLController kml)
	{
		if (dc.isPickingMode())
			return false;

		Boolean b = (Boolean) kml.getValue(AVKey.FEEDBACK_ENABLED);
		return (b != null && b);
	}


	/**
	 * If feedback is enabled for the specified model, this method records feedback about how the specified model has
	 * been processed.
	 *
	 * @param dc the current DrawContext.
	 * @param model the model which the feedback information refers to.
	 * @param modelPoint the model's reference point in model coordinates.
	 * @param screenRect the models's bounding rectangle in screen coordinates.
	 */
	protected void recordFeedback(DrawContext dc, KMLController model, Vec4 modelPoint, Rectangle screenRect)
	{
		if (!this.isFeedbackEnabled(dc, model))
			return;

		this.doRecordFeedback(dc, model, modelPoint, screenRect);
	}


	/**
	 * Records feedback about how the specified WWIcon has been processed.
	 *
	 * @param dc the current DrawContext.
	 * @param icon the icon which the feedback information refers to.
	 * @param modelPoint the icon's reference point in model coordinates.
	 * @param screenRect the icon's bounding rectangle in screen coordinates.
	 */
	@SuppressWarnings({ "UnusedDeclaration" })
	protected void doRecordFeedback(DrawContext dc, KMLController model, Vec4 modelPoint, Rectangle screenRect)
	{
		model.setValue(AVKey.FEEDBACK_REFERENCE_POINT, modelPoint);
		model.setValue(AVKey.FEEDBACK_SCREEN_BOUNDS, screenRect);
	}

}
