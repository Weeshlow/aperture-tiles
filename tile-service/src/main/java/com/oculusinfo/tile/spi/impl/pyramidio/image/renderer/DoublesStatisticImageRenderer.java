/**
 * Copyright (c) 2013 Oculus Info Inc.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tile.spi.impl.pyramidio.image.renderer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.TileSerializer;
import com.oculusinfo.binning.io.impl.DoubleAvroSerializer;
import com.oculusinfo.binning.util.PyramidMetaData;
import com.oculusinfo.tile.spi.impl.IValueTransformer;
import com.oculusinfo.tile.spi.impl.LinearCappedValueTransformer;
import com.oculusinfo.tile.spi.impl.Log10ValueTransformer;
import com.oculusinfo.utilities.imageprocessing.GraphicsUtilities;
import com.oculusinfo.utilities.imageprocessing.StackBlurFilter;

/**
 * An image renderer that works off of tile grids, but instead of rendering
 * a heatmap, calculates some statistics and renders them as text.
 * 
 * @author  dgray
 */
public class DoublesStatisticImageRenderer implements TileDataImageRenderer {
	private  Font FONT = new Font("Tahoma", Font.PLAIN, 13);
	
	private final Logger _logger = LoggerFactory.getLogger(getClass());
	
	private PyramidIO _pyramidIo;
	private TileSerializer<Double> _serializer;


	public DoublesStatisticImageRenderer(PyramidIO pyramidIo) {
		_pyramidIo = pyramidIo;
		_serializer = createSerializer();
	}
	
	protected TileSerializer<Double> createSerializer() {
		return new DoubleAvroSerializer();
	}

	/* (non-Javadoc)
	 * @see com.oculusinfo.tile.spi.impl.pyramidio.image.renderer.TileDataImageRenderer#render(com.oculusinfo.tile.spi.impl.pyramidio.image.renderer.RenderParameter)
	 */
	@Override
	public BufferedImage render(RenderParameter parameter) {
 		BufferedImage bi = null;
 		TileIndex tileIndex = null;
 		String layer = "?";
 		
		try {
			bi = GraphicsUtilities.createCompatibleTranslucentImage(parameter.getOutputWidth(), parameter.getOutputWidth());
		
			tileIndex = parameter.getObject("tileCoordinate", TileIndex.class);
			layer = parameter.getAsString("layer");
			List<TileData<Double>> tileDatas = _pyramidIo.readTiles(layer,
					_serializer, Collections.singleton(tileIndex));
			
			// Missing tiles are commonplace.  We don't want a big long error for that.
			if (tileDatas.size() < 1) {
			    _logger.info("Missing tile " + tileIndex + " for layer " + layer);
			    return null;
			}

			TileData<Double> data = tileDatas.get(0);
			int xBins = data.getDefinition().getXBins();
			int yBins = data.getDefinition().getYBins();
			
			double totalBinCount = 0;
			double maxBinCount = 0;
			double totalNonEmptyBins = 0;
			
			for(int ty = 0; ty < yBins; ty++){
				for(int tx = 0; tx < xBins; tx++){

					double binCount = data.getBin(tx, ty);
					if (binCount > 0 ){
						
						totalNonEmptyBins += 1;
						
						if(binCount > maxBinCount){
							maxBinCount = binCount;
						}
						totalBinCount += binCount;
					}					
				}
			}
			
			double coverage = totalNonEmptyBins/(xBins*yBins);

			DecimalFormat decFormat = new DecimalFormat("");
			String formattedTotal 		= decFormat.format(totalBinCount) + " events   ";
			decFormat = new DecimalFormat("##.##");
			String formattedCoverage 	= decFormat.format(coverage * 100) + "% coverage";
			
			String text = parameter.getAsString("shortName") + ": " + formattedTotal + " " + formattedCoverage;
			drawTextGlow(bi, text, 5, 10, FONT, Color.white, Color.black);
					
		} catch (Exception e) {
			_logger.debug("Tile is corrupt: " + layer + ":" + tileIndex);
			_logger.debug("Tile error: ", e);
			bi = null;
		}
		return bi;
	}
	
	/**
	 * Draw a line of text with a glow around it. Uses fast blurring approximation of gaussian.
	 * TODO: Support calling this multiple times! currently wipes out anything that was there before.
	 * 
	 * @param destination
	 * @param text
	 * @param xOffset
	 * @param yOffset
	 * @param font
	 * @param textColor
	 * @param glowColor
	 */
	private static void drawTextGlow(BufferedImage destination, String text, int xOffset, int yOffset, Font font, Color textColor, Color glowColor) {
		Graphics2D g = destination.createGraphics();
		g.setFont(font);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		FontMetrics fm = g.getFontMetrics();
		Rectangle2D bounds = fm.getStringBounds(text, g);
		FontRenderContext frc = g.getFontRenderContext();
		
		TextLayout layout = new TextLayout(text, g.getFont(), frc);			
		float sw = (float) layout.getBounds().getWidth();
		float sh = (float) layout.getBounds().getHeight();
		Shape shape = layout.getOutline(AffineTransform.getTranslateInstance(
				bounds.getWidth()/2-sw/2 + xOffset, 
				bounds.getHeight()*0.5+sh/2 + yOffset));
		
		BufferedImage biText = GraphicsUtilities.createCompatibleImage(destination);
		Graphics2D gText = biText.createGraphics(); // { gText
			gText.setFont(g.getFont());
			gText.setColor(glowColor);
			gText.setStroke(new BasicStroke(2));
			gText.draw(shape);
		gText.dispose(); // } End gText	
		
		StackBlurFilter blur = new StackBlurFilter(3, 3);
		blur.filter(biText, destination);
		
		g.setColor(textColor);
		g.fill(shape);
		g.dispose();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNumberOfImagesPerTile (PyramidMetaData metadata) {
		// Double tile rendering always produces a single image.
		return 1;
	}

	static class ValueTransformerFactory {
		/**
		 * @param transform
		 * @param levelMaxFreq
		 * @return
		 */
		public static IValueTransformer create(String transform, double levelMaxFreq) {
			IValueTransformer t;
			if("log10".equalsIgnoreCase(transform)){ // TODO: make a factory
				t = new Log10ValueTransformer(levelMaxFreq);
			}else{
				t = new LinearCappedValueTransformer(levelMaxFreq);
			}
			return t;
		}
	}
}