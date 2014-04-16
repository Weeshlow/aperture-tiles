/**
 * Copyright (c) 2013 Oculus Info Inc. http://www.oculusinfo.com/
 * 
 * Released under the MIT License.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tile.rest.tile.caching;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.EmptyConfigurableFactory;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.PyramidIOFactory;
import com.oculusinfo.binning.io.RequestParamsFactory;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.tile.init.FactoryProvider;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rest.tile.TileServiceImpl;
import com.oculusinfo.tile.rest.tile.caching.CachingPyramidIO.LayerDataChangedListener;



/**
 * An ImageTileService implementation that uses caching to allow requesting
 * whole areas at the same time.
 * 
 * @author nkronenfeld
 */
@Singleton
public class CachingTileServiceImpl extends TileServiceImpl {
	private static final Logger LOGGER = LoggerFactory.getLogger(CachingTileServiceImpl.class);



	private FactoryProvider<PyramidIO> _cachingProvider;
	private CachingPyramidIO           _pyramidIO;

	public CachingTileServiceImpl () {
		super();
		_cachingProvider = new CachingPyramidIOProvider();
		_pyramidIO = new CachingPyramidIO();
		_pyramidIO.addLayerListener(new LayerDataChangedListener () {
				public void onLayerDataChanged (String layer) {
					clearMetadataCache(layer);
				}
			});
	}

	@Override
	protected LayerConfiguration getLayerConfiguration () throws ConfigurationException {
		//the root factory that does nothing
		EmptyConfigurableFactory rootFactory = new EmptyConfigurableFactory(null, null, null);
		
		//add another factory that will handle query params
		RequestParamsFactory queryParamsFactory = new RequestParamsFactory(null, rootFactory, Collections.singletonList("query"));
		rootFactory.addChildFactory(queryParamsFactory);
		
		//add the layer configuration factory under the path 'options'
		LayerConfiguration layerConfiguration = new LayerConfiguration(_cachingProvider,
		                              getSerializationFactoryProvider(),
		                              getRendererFactoryProvider(),
		                              rootFactory, Collections.singletonList("options"));
		rootFactory.addChildFactory(layerConfiguration);
		return layerConfiguration;
	}



	@Override
	protected void prepareForRendering (String layer,
	                                    LayerConfiguration config,
	                                    TileIndex tile,
	                                    Iterable<TileIndex> tileSet) {
		try {
			TileSerializer<?> serializer = config.produce(TileSerializer.class);
			_pyramidIO.requestTiles(layer, serializer, tileSet);
		} catch (IOException e) {
			LOGGER.warn("Error requesting tile set", e);
		} catch (ConfigurationException e) {
			LOGGER.warn("Error requesting tile set", e);
		}
	}



	private class CachingPyramidIOFactory extends ConfigurableFactory<PyramidIO> {
		private ConfigurableFactory<?>         _parent;
		private ConfigurableFactory<PyramidIO> _baseFactory;



		CachingPyramidIOFactory (ConfigurableFactory<?> parent,
		                         List<String> path,
		                         ConfigurableFactory<PyramidIO> base) {
			this(null, parent, path, base);
		}

		CachingPyramidIOFactory (String name,
		                         ConfigurableFactory<?> parent,
		                         List<String> path,
		                         ConfigurableFactory<PyramidIO> base) {
			super(name, PyramidIO.class, parent, path);
			_parent = parent;
			_baseFactory = base;

			addProperty(PyramidIOFactory.INITIALIZATION_DATA);
		}

		@Override
		public void readConfiguration (JSONObject rootNode) throws ConfigurationException {
			super.readConfiguration(rootNode);
			_baseFactory.readConfiguration(rootNode);
			setupBasePyramidIO();
		}

		@Override
		public void readConfiguration (Properties properties) throws ConfigurationException {
			super.readConfiguration(properties);
			_baseFactory.readConfiguration(properties);
			setupBasePyramidIO();
		}

		private void setupBasePyramidIO () {
			String pyramidId = _parent.getPropertyValue(LayerConfiguration.LAYER_NAME);
			_pyramidIO.setupBasePyramidIO(pyramidId, _baseFactory);
		}

		@Override
		protected PyramidIO create () {
			return _pyramidIO;
		}
        
	}
	private class CachingPyramidIOProvider implements FactoryProvider<PyramidIO> {
		@Override
		public ConfigurableFactory<PyramidIO> createFactory (List<String> path) {
			return new CachingPyramidIOFactory(null, path, getPyramidIOFactoryProvider().createFactory(path));
		}

		@Override
		public ConfigurableFactory<PyramidIO> createFactory (ConfigurableFactory<?> parent,
		                                                     List<String> path) {
			return new CachingPyramidIOFactory(parent, path, getPyramidIOFactoryProvider().createFactory(parent, path));
		}

		@Override
		public ConfigurableFactory<PyramidIO> createFactory (String factoryName,
		                                                     ConfigurableFactory<?> parent,
		                                                     List<String> path) {
			return new CachingPyramidIOFactory(parent, path, getPyramidIOFactoryProvider().createFactory(factoryName, parent, path));
		}
	}
}
