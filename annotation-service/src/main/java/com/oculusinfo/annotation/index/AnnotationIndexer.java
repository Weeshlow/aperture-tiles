/*
 * Copyright (c) 2014 Oculus Info Inc. http://www.oculusinfo.com/
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
package com.oculusinfo.annotation.index;

import java.util.LinkedList;
import java.util.List;

import com.oculusinfo.binning.*;
import com.oculusinfo.annotation.*;

public abstract class AnnotationIndexer<T> {

    protected static final int NUM_LEVELS = 20;
    
    protected TilePyramid _pyramid;
    
    public AnnotationIndexer() {
    }
    
    public List<T> getIndices( AnnotationData data ) {
    	List<T> indices = new LinkedList<>();		
		for (int i=0; i<NUM_LEVELS; i++) {
			indices.add( getIndex( data, i ) );
		}
		return indices;
    }
    
    public abstract T getIndex( AnnotationData data, int level );

    
}
