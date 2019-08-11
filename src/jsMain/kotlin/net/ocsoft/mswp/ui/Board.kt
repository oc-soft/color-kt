package net.ocsoft.mswp.ui

import org.khronos.webgl.*
import net.ocsoft.mswp.*

/**
 * game board
 */
class Board(
    var color: FloatArray  = ColorScheme.colors[4]) {

    /**
     * cache of vertices
     */
    private var verticesCache : FloatArray? = null
   
    val vertices : FloatArray
        get() {
            if (verticesCache == null) {
                verticesCache = 
                    Polygon.divideSquare2(size,
                        polygonFactor[0].toInt(), 
                        polygonFactor[1].toInt())
            }
            return verticesCache!!
        }
 
    val verticesAsFloat32 : Float32Array
        get() {
            val vertices = this.vertices
            val result = Float32Array(
                Array<Float>(vertices.size) { i -> vertices[i] })
            return result
        }

    /**
     * normal vector's cache
     */
    private var normalVecCache : FloatArray? = null
    
    /**
     * normal vectors
     */
    val normalVectors : FloatArray
        get() {
            if (normalVecCache == null) {
                normalVecCache = 
                    Polygon.createNormalVectorsForTriangles(vertices)
            }
            return normalVecCache!!
        }
    val normalVectorsAsFloat32 : Float32Array
        get() {
            val normalVectors = this.normalVectors 
            val result = Float32Array(Array<Float>(normalVectors.size) {
                i -> normalVectors[i]
            })
            return result 
 
        }

    /**
     * vertex colors
     */
    val verticesColor : FloatArray
        get() {
            val vertices = this.vertices
            val color = this.color
            return FloatArray((vertices.size / 3) * color.size) {
                i -> color[i % color.size] 
            }
        }
    val verticesColorAsFloat32 : Float32Array
        get() {
            val verticesColor = this.verticesColor
            val result = Float32Array(Array<Float>(verticesColor.size) {
                i -> verticesColor[i]
            })
            return result 
        }

    
    /**
     * board size
     */
    val size : FloatArray = floatArrayOf(1.0f, 1.0f)

    /**
     * polygon factor
     */
    val polygonFactor = shortArrayOf(1, 1)

    /**
     * drawing mode
     */
    val drawingMode = WebGLRenderingContext.TRIANGLES


    /**
     * create vertices color
     */
    fun createVerticesColor(color: FloatArray) : Float32Array {
        val verticesColor = Array<Float>((vertices.size / 3) * color.size) {
            i ->color[i % color.size]
        }
        return Float32Array(verticesColor)
    }
    
}