/*
 * Copyright (c) 2017 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.draw;

import android.opengl.GLES20;

import gov.nasa.worldwind.geom.Matrix4;
import gov.nasa.worldwind.util.Pool;

public class DrawableSurfaceShapeShadowVolume implements Drawable {

    public DrawShapeState drawState = new DrawShapeState();

    private Matrix4 mvpMatrix = new Matrix4();

    private static final Matrix4 UNIT_SQUARE_TRANSFORM = new Matrix4();

    private Pool<DrawableSurfaceShapeShadowVolume> pool;

    public DrawableSurfaceShapeShadowVolume() {
    }

    public static DrawableSurfaceShapeShadowVolume obtain(Pool<DrawableSurfaceShapeShadowVolume> pool) {
        DrawableSurfaceShapeShadowVolume instance = pool.acquire(); // get an instance from the pool
        return (instance != null) ? instance.setPool(pool) : new DrawableSurfaceShapeShadowVolume().setPool(pool);
    }

    private DrawableSurfaceShapeShadowVolume setPool(Pool<DrawableSurfaceShapeShadowVolume> pool) {
        this.pool = pool;
        return this;
    }

    @Override
    public void recycle() {
        this.drawState.reset();

        if (this.pool != null) { // return this instance to the pool
            this.pool.release(this);
            this.pool = null;
        }
    }

    @Override
    public void draw(DrawContext dc) {

        // configure buffers
        GLES20.glColorMask(false, false, false, false);
        GLES20.glDepthMask(false);
        GLES20.glEnable(GLES20.GL_STENCIL_TEST);
        GLES20.glClear(GLES20.GL_STENCIL_BUFFER_BIT);

        // configure stencil
        GLES20.glCullFace(GLES20.GL_FRONT);
        GLES20.glStencilFunc(GLES20.GL_ALWAYS, 0, 0xff);
        GLES20.glStencilOp(GLES20.GL_KEEP, GLES20.GL_INCR, GLES20.GL_KEEP);

        this.drawShadowVolume(dc);

        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glStencilOp(GLES20.GL_KEEP, GLES20.GL_DECR, GLES20.GL_KEEP);

        this.drawShadowVolume(dc);

        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glColorMask(true, true, true, true);
        GLES20.glDepthMask(true);
        GLES20.glStencilFunc(GLES20.GL_NOTEQUAL, 0x0, 0xFF);
        GLES20.glStencilOp(GLES20.GL_REPLACE, GLES20.GL_REPLACE, GLES20.GL_REPLACE);

        this.drawShadow(dc);

        GLES20.glDisable(GLES20.GL_STENCIL_TEST);
    }

    protected void drawShadowVolume(DrawContext dc) {
        if (this.drawState.program == null || !this.drawState.program.useProgram(dc)) {
            return; // program unspecified or failed to build
        }

        if (this.drawState.vertexBuffer == null || !this.drawState.vertexBuffer.bindBuffer(dc)) {
            return; // vertex buffer unspecified or failed to bind
        }

        if (this.drawState.elementBuffer == null || !this.drawState.elementBuffer.bindBuffer(dc)) {
            return; // element buffer unspecified or failed to bind
        }

        // Load the model view projection matrix
        this.drawState.program.loadModelviewProjection(this.mvpMatrix);

        GLES20.glEnableVertexAttribArray(1 /*vertexPointLow*/);
        GLES20.glVertexAttribPointer(0 /*vertexPointHigh*/, 3, GLES20.GL_FLOAT, false, this.drawState.vertexStride, 0 /*offset*/);

        // Draw the specified primitives.
        for (int idx = 0; idx < this.drawState.primCount; idx++) {
            DrawShapeState.DrawElements prim = this.drawState.prims[idx];
            this.drawState.program.loadColor(prim.color);
            this.drawState.program.enableTexture(false);

            GLES20.glVertexAttribPointer(1 /*vertexPointLow*/, 3, GLES20.GL_FLOAT, false, this.drawState.vertexStride, 1);
            GLES20.glDrawElements(prim.mode, prim.count, prim.type, prim.offset);
        }

        // Restore the default WorldWind OpenGL state.
        if (!this.drawState.enableCullFace) {
            GLES20.glEnable(GLES20.GL_CULL_FACE);
        }
        if (!this.drawState.enableDepthTest) {
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        }
        GLES20.glLineWidth(1);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glDisableVertexAttribArray(1 /*vertexPointLow*/);
    }

    protected void drawShadow(DrawContext dc) {

        UNIT_SQUARE_TRANSFORM.setToIdentity();
        UNIT_SQUARE_TRANSFORM.setTranslation(0, 0, 0);
        UNIT_SQUARE_TRANSFORM.multiplyByScale(dc.viewport.x, dc.viewport.y, 1);

        if (!dc.unitSquareBuffer().bindBuffer(dc)) {
            return; // vertex buffer failed to bind
        }

        // Use a unit square as the vertex point
        GLES20.glVertexAttribPointer(0 /*vertexPoint*/, 2, GLES20.GL_FLOAT, false, 0, 0);

        // Draw this DrawableScreenTextures.
        // Use the drawable's color.
        this.drawState.program.loadColor(this.drawState.color);

        this.drawState.program.enableTexture(false);

        // Use a modelview-projection matrix that transforms the unit square to screen coordinates.
        this.mvpMatrix.setToMultiply(dc.screenProjection, UNIT_SQUARE_TRANSFORM);
        this.drawState.program.loadModelviewProjection(this.mvpMatrix);

        // Draw the unit square as triangles.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Restore the default WorldWind OpenGL state.
        if (!this.drawState.enableCullFace) {
            GLES20.glEnable(GLES20.GL_CULL_FACE);
        }
        if (!this.drawState.enableDepthTest) {
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        }
        GLES20.glLineWidth(1);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glDisableVertexAttribArray(1 /*vertexPointLow*/);
    }
}
