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

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/* JSLint global declarations: these objects don't need to be declared. */
/*global OpenLayers */

/**
 * This module defines a simple client-rendered layer that displays a 
 * text score tile in a meaningful way.
 */
define(function (require) {
    "use strict";



    var MapLayer = require('./MapLayer'),
        MapServerCoordinator = require('./MapServerCoordinator'),
        TextScoreLayer;



    TextScoreLayer = MapLayer.extend({
        ClassName: "TextScoreLayer",
        init: function (id, layerSpec) {
            this._super(id);
            this.tracker.setPosition('center');
            this.coordinator = new MapServerCoordinator(this.tracker,
                                                        layerSpec);
        },

        createLayer: function (nodeLayer) {
            // Store our coordinator locally so that we can get to metainfo
            // from within our aperture property functions; within the functions,
            // <em>this</em> refers to the data, so we need closures to get
            // information about the real <em>this</em>
            var coordinatorLocal = this.coordinator,
                maxSeen = Number.MIN_VALUE,
                getProportionOfMax = function (value, level) {
                    var max = maxSeen;
                    if (level &&
                        coordinatorLocal &&
                        coordinatorLocal.layerInfo &&
                        coordinatorLocal.layerInfo.meta &&
                        coordinatorLocal.layerInfo.meta.levelMaximums &&
                        coordinatorLocal.layerInfo.meta.levelMaximums[level] &&
                        isFinite(coordinatorLocal.layerInfo.meta.levelMaximums[level])) {
                        max = coordinatorLocal.layerInfo.meta.levelMaximums[level];
                    } else if (Math.abs(value) > max) {
                        max = Math.abs(value);
                    }

                    if (max > maxSeen) {
                        maxSeen = max;
                    }
                    return value / max;
                };

            this.labelLayer = this._nodeLayer.addLayer(aperture.LabelLayer);
            this.labelLayer.map('label-count').from('bin.value.length');
            this.labelLayer.map('text').from(function (index) {
                return this.bin.value[index].key;
            });
            this.labelLayer.map('offset-y').from(function (index) {
                return 16 * (index - (this.bin.value.length - 1.0) / 2);
            });
            this.labelLayer.map('text-anchor').from(function (index) {
                var value = getProportionOfMax(this.bin.value[index].value, this.level);
                if (value >= 0) {
                    return 'end';
                }
                return 'start';
            });
            this.labelLayer.map('fill').asValue('#FFF');
            this.labelLayer.map('font-outline').asValue('#222');
            this.labelLayer.map('font-outline-width').asValue(3);
            this.labelLayer.map('visible').asValue(true);

            this.barLayer = this._nodeLayer.addLayer(aperture.BarLayer);
            this.barLayer.map('orientation').asValue('horizontal');
            this.barLayer.map('bar-count').from('bin.value.length');
            this.barLayer.map('x').asValue(0);
            this.barLayer.map('y').from(function (index) {
                return 16 * (index - (this.bin.value.length - 1.0) / 2);
            });
            this.barLayer.map('width').asValue('10');
            this.barLayer.map('length').from(function (index) {
                var value = getProportionOfMax(this.bin.value[index].value);
                return value * 150.0;
            });
            this.barLayer.map('fill').from('#80C0FF');

            this.coordinator.setMap(this.map);
        }
    });

    return TextScoreLayer;
});