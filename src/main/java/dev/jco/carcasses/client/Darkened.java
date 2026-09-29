package dev.jco.carcasses.client;
import com.mojang.blaze3d.vertex.VertexConsumer;
public final class Darkened implements VertexConsumer {
        private final VertexConsumer delegate;private final float amount;
        public Darkened(VertexConsumer delegate,float amount){this.delegate=delegate;this.amount=amount;}
        public VertexConsumer addVertex(float x,float y,float z){delegate.addVertex(x,y,z);return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){
            float cooked=Math.clamp(amount,0,1);
            delegate.setColor((int)(r*(1-.35*cooked)),(int)(g*(1-.50*cooked)),(int)(b*(1-.63*cooked)),a);
            return this;
        }
        public VertexConsumer setUv(float u,float v){delegate.setUv(u,v);return this;}
        public VertexConsumer setUv1(int u,int v){delegate.setUv1(u,v);return this;}
        public VertexConsumer setUv2(int u,int v){delegate.setUv2(u,v);return this;}
        public VertexConsumer setNormal(float x,float y,float z){delegate.setNormal(x,y,z);return this;}
    }
