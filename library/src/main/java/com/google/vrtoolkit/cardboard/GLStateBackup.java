package com.google.vrtoolkit.cardboard;

import java.nio.*;
import android.opengl.*;
import java.util.*;

class GLStateBackup {
    private boolean mCullFaceEnabled;
    private boolean mScissorTestEnabled;
    private boolean mDepthTestEnabled;
    private IntBuffer mViewport;
    private IntBuffer mTexture2dId;
    private IntBuffer mTextureUnit;
    private IntBuffer mScissorBox;
    private IntBuffer mShaderProgram;
    private IntBuffer mArrayBufferBinding;
    private IntBuffer mElementArrayBufferBinding;
    private FloatBuffer mClearColor;
    private ArrayList<VertexAttributeState> mVertexAttributes;
    
    GLStateBackup() {
        super();
        this.mViewport = IntBuffer.allocate(4);
        this.mTexture2dId = IntBuffer.allocate(1);
        this.mTextureUnit = IntBuffer.allocate(1);
        this.mScissorBox = IntBuffer.allocate(4);
        this.mShaderProgram = IntBuffer.allocate(1);
        this.mArrayBufferBinding = IntBuffer.allocate(1);
        this.mElementArrayBufferBinding = IntBuffer.allocate(1);
        this.mClearColor = FloatBuffer.allocate(4);
        this.mVertexAttributes = new ArrayList<VertexAttributeState>();
    }
    
    void addTrackedVertexAttribute(final int attributeId) {
        this.mVertexAttributes.add(new VertexAttributeState(attributeId));
    }
    
    void clearTrackedVertexAttributes() {
        this.mVertexAttributes.clear();
    }
    
    void readFromGL() {
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, this.mViewport);
        this.mCullFaceEnabled = GLES20.glIsEnabled(GLES20.GL_CULL_FACE);
        this.mScissorTestEnabled = GLES20.glIsEnabled(GLES20.GL_SCISSOR_TEST);
        this.mDepthTestEnabled = GLES20.glIsEnabled(GLES20.GL_DEPTH_TEST);
        GLES20.glGetFloatv(GLES20.GL_COLOR_CLEAR_VALUE, this.mClearColor);
        GLES20.glGetIntegerv(GLES20.GL_CURRENT_PROGRAM, this.mShaderProgram);
        GLES20.glGetIntegerv(GLES20.GL_SCISSOR_BOX, this.mScissorBox);
        GLES20.glGetIntegerv(GLES20.GL_ACTIVE_TEXTURE, this.mTextureUnit);
        GLES20.glGetIntegerv(GLES20.GL_TEXTURE_BINDING_2D, this.mTexture2dId);
        GLES20.glGetIntegerv(GLES20.GL_ARRAY_BUFFER_BINDING, this.mArrayBufferBinding);
        GLES20.glGetIntegerv(GLES20.GL_ELEMENT_ARRAY_BUFFER_BINDING, this.mElementArrayBufferBinding);
        for (final VertexAttributeState vas : this.mVertexAttributes) {
            vas.readFromGL();
        }
    }
    
    void writeToGL() {
        for (final VertexAttributeState vas : this.mVertexAttributes) {
            vas.writeToGL();
        }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, this.mArrayBufferBinding.array()[0]);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, this.mElementArrayBufferBinding.array()[0]);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, this.mTexture2dId.array()[0]);
        GLES20.glActiveTexture(this.mTextureUnit.array()[0]);
        GLES20.glScissor(this.mScissorBox.array()[0], this.mScissorBox.array()[1], this.mScissorBox.array()[2], this.mScissorBox.array()[3]);
        GLES20.glUseProgram(this.mShaderProgram.array()[0]);
        GLES20.glClearColor(this.mClearColor.array()[0], this.mClearColor.array()[1], this.mClearColor.array()[2], this.mClearColor.array()[3]);
        if (this.mCullFaceEnabled) {
            GLES20.glEnable(GLES20.GL_CULL_FACE);
        }
        else {
            GLES20.glDisable(GLES20.GL_CULL_FACE);
        }
        if (this.mScissorTestEnabled) {
            GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        }
        else {
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
        }
        if (this.mDepthTestEnabled) {
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        }
        else {
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        }
        GLES20.glViewport(this.mViewport.array()[0], this.mViewport.array()[1], this.mViewport.array()[2], this.mViewport.array()[3]);
    }
    
    private class VertexAttributeState
    {
        private int mAttributeId;
        private IntBuffer mEnabled;
        
        VertexAttributeState(final int attributeId) {
            super();
            this.mEnabled = IntBuffer.allocate(1);
            this.mAttributeId = attributeId;
        }
        
        void readFromGL() {
            GLES20.glGetVertexAttribiv(this.mAttributeId, GLES20.GL_VERTEX_ATTRIB_ARRAY_ENABLED, this.mEnabled);
        }
        
        void writeToGL() {
            if (this.mEnabled.array()[0] == 0) {
                GLES20.glDisableVertexAttribArray(this.mAttributeId);
            }
            else {
                GLES20.glEnableVertexAttribArray(this.mAttributeId);
            }
        }
    }
}
