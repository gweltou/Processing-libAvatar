package gwel.game.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Bezier;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;
import gwel.game.anim.Animation;
import gwel.game.utils.DummyPShape;
import processing.core.PApplet;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.core.PVector;


import java.util.ArrayList;

import static gwel.game.graphics.Utils.pColorToGDXColor;
import static processing.core.PConstants.*;


public class ComplexShape implements Shape {
    protected ComplexShape parent;
    private final ArrayList<ComplexShape> children;
    private final ArrayList<Shape> shapes; // CompleShapes and simple shapes
    private Animation[] animations;
    private final Vector2 localOrigin;  // Pivot point
    private final Affine2 transform, oldTransform, nextTransform;
    private String id = "";
    private boolean transitioning = false;
    private float transitionDuration;
    private float transitionTime;
    private float[] tint, colorMod;

    public ComplexShape() {
        children = new ArrayList<>();
        shapes = new ArrayList<>();
        localOrigin = new Vector2();
        animations = new Animation[0];
        transform = new Affine2();
        oldTransform = new Affine2();
        nextTransform = new Affine2();
        tint = new float[] {0f, 0f, 0f, 1f};
        colorMod = new float[] {0f, 0f, 0f, 1f};
    }

    public void addShape(Shape shape) {
        shapes.add(shape);
        if (shape instanceof ComplexShape) {
            children.add((ComplexShape) shape);
            ((ComplexShape) shape).parent = this;
        }
    }

    // Return all chilren (Drawables and ComplexShapes)
    public ArrayList<Shape> getShapes() {
        return shapes;
    }

    // Return only complexShape children
    public ArrayList<ComplexShape> getChildren() {
        return children;
    }

    // Remove all children
    public void clear() {
        children.clear();
        shapes.clear();
    }


    public Vector2 getLocalOrigin() {
        return localOrigin.cpy();
    }

    public void setLocalOrigin(float x, float y) {
        localOrigin.set(x, y);
    }


    /**
     * Not implemented
     */
    public boolean contains(float x, float y) {
        return false;
    }


    public Affine2 getTransform() {
        return new Affine2(transform);
    }

    public Affine2 getAbsoluteTransform() {
        Affine2 mat = new Affine2(transform);
        if (parent == null) {
            return mat;
        } else {
            return mat.preMul(parent.getAbsoluteTransform());
        }
    }


    public void hardTransform(Affine2 transform) {
        transform.applyTo(localOrigin);
        for (Shape shape : shapes)
            shape.hardTransform(transform);
        if (transform.m00 != 0) {
            // Must scale animation tree
            for (Animation animation : animations)
                animation.scale(transform.m00);
        }
    }

    public void hardTranslate(float x, float y) {
        hardTransform(new Affine2().setToTranslation(x, y));
    }

    public void hardScale(float sx, float sy) {
        hardTransform(new Affine2().setToScaling(sx, sy));
    }


    public String getId() {
        return id;
    }

    public void setId(String label) {
        id = label;
    }


    public ComplexShape getById(String label) {
        if (id.equals(label))
            return this;
        for (ComplexShape child : children) {
            if (child.getById(label) != null)
                return child.getById(label);
        }
        return null;
    }


    public ArrayList<String> getIdList() {
        ArrayList<String> list = new ArrayList<>();
        list.add(id);
        for (ComplexShape child : children)
            list.addAll(child.getIdList());
        return list;
    }

    public ArrayList<String> getIdListPre(String pre) {
        ArrayList<String> list = new ArrayList<>();
        list.add(pre + id);
        for (ComplexShape child : children) {
            for (String childId : child.getIdListPre("  ")) list.add(pre + childId);
        }
        return list;
    }


    public ArrayList<ComplexShape> getPartsList() {
        ArrayList<ComplexShape> list = new ArrayList<>();
        list.add(this);
        for (ComplexShape child : children) {
            list.addAll(child.getPartsList());
        }
        return list;
    }


    public Animation[] getAnimationList() {
        return animations;
    }

    public void setAnimationList(Animation[] animList) {
        animations = animList;
    }

    public void clearAnimationList() {
        transform.idt();
        animations = new Animation[0];
    }


    public Animation getAnimation(int n) {
        return animations[n];
    }

    public void setAnimation(int i, Animation anim) {
        animations[i] = anim;
    }

    public void addAnimation(Animation anim) {
        Animation[] newAnimations = new Animation[animations.length + 1];
        System.arraycopy(animations, 0, newAnimations, 0, animations.length);
        newAnimations[animations.length] = anim;
        animations = newAnimations;
    }

    public void removeAnimation(int idx) {
        Animation[] newAnimations = new Animation[animations.length - 1];
        int i = 0;
        for (int n = 0; n < newAnimations.length; n++) {
            if (i == idx)
                i++;
            newAnimations[n] = animations[i++];
        }
        animations = newAnimations;
    }


    public void update(float dtime) {
        // dtime is in seconds

        if (transitioning) {
            transitionTime += dtime;
            float t = transitionTime / transitionDuration;
            if (transitionTime >= transitionDuration) {
                transitioning = false;
                t = 1.0f;
            }
            transform.setToTranslation(localOrigin);
            transform.mul(Animation.lerpAffine(oldTransform, nextTransform, t));
            transform.translate(-localOrigin.x, -localOrigin.y);
        } else if (animations.length > 0) {
            transform.setToTranslation(localOrigin);
            System.arraycopy(tint, 0, colorMod, 0, tint.length);
            for (int i = animations.length - 1; i >= 0; i--) {
                animations[i].update(dtime);
                if (animations[i].getAxe() < 6) {
                    transform.mul(animations[i].getTransform());
                } else {
                    float[] animColorMod = animations[i].getColorMod();
                    colorMod[0] += animColorMod[0];
                    colorMod[1] += animColorMod[1];
                    colorMod[2] += animColorMod[2];
                    colorMod[3] *= animColorMod[3];
                }
            }
            transform.translate(-localOrigin.x, -localOrigin.y);
        }

        for (ComplexShape child : children)
            child.update(dtime);
    }

    public void reset() {
        if (animations.length > 0) {
            for (Animation anim : animations) {
                anim.reset();
            }
            transform.idt();
        }
        for (ComplexShape child : children)
            child.reset();
    }


    // Returns true if all animations are running
    public boolean isAnimationRunning() {
        boolean running = true;
        for (Animation animation : animations) {
            running = running && animation.isRunning();
        }
        return running;
    }

    // Returns true if any animation is stopped
    public boolean isAnimationStopped() {
        for (Animation animation : animations) {
            if (animation.isStopped())
                return true;
        }
        return false;
    }


    // Maybe could put a "fixtransform" parameter here
    public void transitionAnimation(Animation[] nextAnims, float duration) {
        // duration is in seconds
        if (animations.length > 0) {
            transitioning = true;
            transitionDuration = duration;
            transitionTime = 0;
            oldTransform.idt();
            for (Animation anim : animations) {
                oldTransform.preMul(anim.getTransform());
            }
            nextTransform.idt();
            for (Animation anim : nextAnims) {
                nextTransform.preMul(anim.getTransform());
            }
        }
        animations = nextAnims;
    }


    public void setColorMod(float mr, float mg, float mb, float ma) {
        tint[0] = mr;
        tint[1] = mg;
        tint[2] = mb;
        tint[3] = ma;
    }

    public void setColorMod(Color colorMod) {
        tint[0] = colorMod.r;
        tint[1] = colorMod.g;
        tint[2] = colorMod.b;
        tint[3] = colorMod.a;
    }


    public void draw(MyRenderer renderer) {
        renderer.pushMatrix(transform);
        renderer.pushColorMod(colorMod);
        for (Drawable shape : shapes)
            shape.draw(renderer);
        renderer.popMatrix();
        renderer.popColorMod();
    }


    // Used for processing animation editor
    public void drawSelected(MyRenderer renderer) {
        renderer.pushMatrix(transform);
        for (Drawable shape : shapes)
            shape.drawSelected(renderer);
        renderer.popMatrix();
    }

    // Used for processing animation editor
    public void drawSelectedOnly(MyRenderer renderer) {
        if (renderer.getSelected() == this) {
            // Highlight this shape
            drawSelected(renderer);
        } else {
            renderer.pushMatrix(transform);
            for (ComplexShape shape : children)
                shape.drawSelectedOnly(renderer);
            renderer.popMatrix();
        }
    }


    public ComplexShape copy() {
        ComplexShape copy = new ComplexShape();
        for (Shape shape : shapes)
            copy.addShape(shape.copy());
        copy.setLocalOrigin(localOrigin.x, localOrigin.y);
        Animation[] animList = new Animation[animations.length];
        for (int i=0; i<animList.length; i++)
            animList[i] = animations[i].copy();
        copy.setAnimationList(animList);

        return copy;
    }


    // Used for processing animation editor
    public static ComplexShape fromPShape(PShape svgShape) {
        Shape shape = fromPShape(svgShape, new PMatrix3D(), 0);
        if (!(shape instanceof ComplexShape)) {
            ComplexShape cs = new ComplexShape();
            cs.addShape(shape);
            return cs;
        }
        return (ComplexShape) shape;
    }

    // Used for processing animation editor
    public static Shape fromPShape(PShape svgShape, PMatrix3D matrix, int depth) {
        StringBuilder prefix = new StringBuilder();

        for (int i=0; i<depth; i++)
            prefix.append('-');

        Shape shape = null;
        int family = svgShape.getFamily();
        int kind = svgShape.getKind();
        int childCount = svgShape.getChildCount();
        PMatrix3D mat = (PMatrix3D) (new DummyPShape(svgShape)).getMatrix();
        if (mat != null)
            matrix.apply(mat);

        if (childCount > 0) {
            ComplexShape cs = new ComplexShape();
            cs.setId(svgShape.getName());
            for (PShape child : svgShape.getChildren()) {
                Shape childShape = fromPShape(child, matrix.get(), depth+1);
                if (childShape != null)
                    cs.addShape(childShape);
            }
            shape = cs;
        }
        else if (family == PShape.PATH) {
            int vertexCount = svgShape.getVertexCount();
            int[] vertexCodes = svgShape.getVertexCodes();
            ArrayList<Float> verts = new ArrayList<Float>(); // vertex buffer for polygon

            if (svgShape.getVertexCodeCount() == 0) {  // each point is a simple vertex
                for (int i = 0; i < vertexCount; i++) {
                    PVector vertex = new PVector(svgShape.getVertexX(i), svgShape.getVertexY(i));
                    vertex = matrix.mult(vertex, null);
                    verts.add(vertex.x);
                    verts.add(vertex.y);
                }
            } else {  // coded set of vertices
                int codeIdx = 0;
                Vector2 prev, ctrl, ctrl2, end, point;
                PVector ppoint;
                Bezier<Vector2> b;
                for (int j = 0; j < svgShape.getVertexCodeCount(); j++) {
                    switch (vertexCodes[j]) {
                        case VERTEX:
                            PVector vertex = new PVector(svgShape.getVertexX(codeIdx), svgShape.getVertexY(codeIdx));
                            vertex = matrix.mult(vertex, null);
                            verts.add(vertex.x);
                            verts.add(vertex.y);
                            codeIdx++;
                            break;

                        case QUADRATIC_VERTEX:
                            prev = new Vector2(svgShape.getVertexX(codeIdx-1), svgShape.getVertexY(codeIdx-1));
                            ctrl = new Vector2(svgShape.getVertexX(codeIdx), svgShape.getVertexY(codeIdx));
                            end = new Vector2(svgShape.getVertexX(codeIdx+1), svgShape.getVertexY(codeIdx+1));
                            b = new Bezier<>(prev, ctrl, end);
                            // Approximate quadratic Bezier curve (8 steps)
                            point = new Vector2();
                            for (int i=0; i<=6; i++) {
                                b.valueAt(point, (float) i/6);
                                ppoint = matrix.mult(new PVector(point.x, point.y), null);
                                verts.add(ppoint.x);
                                verts.add(ppoint.y);
                            }
                            codeIdx += 2;
                            break;

                        case BEZIER_VERTEX:
                            prev = new Vector2(svgShape.getVertexX(codeIdx-1), svgShape.getVertexY(codeIdx-1));
                            ctrl = new Vector2(svgShape.getVertexX(codeIdx), svgShape.getVertexY(codeIdx));
                            ctrl2 = new Vector2(svgShape.getVertexX(codeIdx+1), svgShape.getVertexY(codeIdx+1));
                            end = new Vector2(svgShape.getVertexX(codeIdx+2), svgShape.getVertexY(codeIdx+2));
                            b = new Bezier(prev, ctrl, ctrl2, end);
                            // Approximate Bezier curve (8 steps)
                            point = new Vector2();
                            for (int i=0; i<=8; i++) {
                                b.valueAt(point, (float) i/8);
                                ppoint = matrix.mult(new PVector(point.x, point.y), null);
                                verts.add(ppoint.x);
                                verts.add(ppoint.y);
                            }
                            codeIdx += 3;
                            break;

                        case BREAK:
                            System.out.println("BREAK");
                            /*
                            if (insideContour) {
                                g.endContour();
                            }
                            g.beginContour();
                            insideContour = true;
                         */
                    }
                }
            }
            /*
            if (insideContour) {
                g.endContour();
            }*/

            float[] verticesArray = new float[verts.size()];
            for (int i=0; i<verticesArray.length; i++)
                verticesArray[i] = verts.get(i);
            DrawablePolygon poly = new DrawablePolygon(verticesArray);
            try { poly.setColor(pColorToGDXColor(svgShape.getFill(999))); }
            catch (Exception e) { poly.setColor(0.f, 0.f, 0.f, 1.f); e.printStackTrace(); }
            shape = poly;
        }
        else if (family == PShape.PRIMITIVE ) {
            float[] params = svgShape.getParams();
            if (kind == PShape.ELLIPSE) {
                float r = params[2];
                // params[0], params[1] is top-left coordinate
                PVector center = matrix.mult(new PVector(params[0]+r/2, params[1]+r/2), null);
                PVector radiusPoint = matrix.mult(new PVector(params[0]+ r, 0), null);
                DrawableCircle c = new DrawableCircle(center.x, center.y, radiusPoint.x-center.x);
                try { c.setColor(pColorToGDXColor(svgShape.getFill(0))); }
                catch (Exception e) { c.setColor(0.f, 0.f, 0.f, 1.f); e.printStackTrace(); }
                shape = c;
            }
            else if (kind == PShape.RECT) {
                float x = params[0];
                float y = params[1];
                float width = params[2];
                float height = params[3];
                PVector p0 = matrix.mult(new PVector(x, y), null);
                PVector p1 = matrix.mult(new PVector(x+width, y), null);
                PVector p2 = matrix.mult(new PVector(x+width, y+height), null);
                PVector p3 = matrix.mult(new PVector(x, y+height), null);
                float[] verts = new float[] {p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y};
                DrawablePolygon poly = new DrawablePolygon(verts);
                try { poly.setColor(pColorToGDXColor(svgShape.getFill(999))); }
                catch (Exception e) { poly.setColor(0.f, 0.f, 0.f, 1.f); e.printStackTrace(); }
                shape = poly;
            }
        }
        return shape;
    }


    public static ComplexShape fromJson(JsonValue json) {
        ComplexShape cs = new ComplexShape();
        cs.setId(json.getString("id"));
        if (json.has("shapes")) {
            for (JsonValue shape : json.get("shapes")) {
                if (shape.has("type")) {  // Treat as simple shape
                    String type = shape.getString("type");
                    if (type.equals("polygon")) {
                        DrawablePolygon p = new DrawablePolygon();
                        p.setVertices(shape.get("vertices").asFloatArray());
                        p.setIndices(shape.get("triangles").asShortArray());
                        float[] c = shape.get("color").asFloatArray();
                        p.setColor(c[0], c[1], c[2], c[3]);

                        cs.addShape(p);
                    } else if (type.equals("circle")) {
                        float[] params = shape.get("params").asFloatArray();
                        DrawableCircle c = new DrawableCircle(params[0], params[1], params[2]);
                        float[] co = shape.get("color").asFloatArray();
                        c.setColor(co[0], co[1], co[2], co[3]);
                        cs.addShape(c);
                    }
                } else if (shape.has("id")) {  // Treat as ComplexShape
                    cs.addShape(fromJson(shape));
                }
            }
        }

        if (json.has("origin")) {
            float[] coord = json.get("origin").asFloatArray();
            cs.setLocalOrigin(coord[0], coord[1]);
        }

        if (json.has("animation")) {
            for (JsonValue animJson : json.get("animation").iterator())
                cs.addAnimation(Animation.fromJson(animJson));
        }

        return cs;
    }


    public JsonValue toJson(boolean saveAnim) {
        JsonValue json = new JsonValue(JsonValue.ValueType.object);
        json.addChild("id", new JsonValue(id));

        JsonValue origin = new JsonValue(JsonValue.ValueType.array);
        origin.addChild(new JsonValue(localOrigin.x));
        origin.addChild(new JsonValue(localOrigin.y));
        json.addChild("origin", origin);

        JsonValue shapes = new JsonValue(JsonValue.ValueType.array);
        for (Drawable shape : getShapes()) {
            if (shape instanceof ComplexShape) {
                shapes.addChild(((ComplexShape) shape).toJson(saveAnim));
            } else if (shape instanceof DrawablePolygon) {
                DrawablePolygon p = (DrawablePolygon) shape;
                JsonValue s = new JsonValue(JsonValue.ValueType.object);
                s.addChild("type", new JsonValue("polygon"));

                JsonValue colorArray = new JsonValue(JsonValue.ValueType.array);
                colorArray.addChild(new JsonValue(p.getColor().r));
                colorArray.addChild(new JsonValue(p.getColor().g));
                colorArray.addChild(new JsonValue(p.getColor().b));
                colorArray.addChild(new JsonValue(p.getColor().a));
                s.addChild("color", colorArray);

                JsonValue verticesArray = new JsonValue(JsonValue.ValueType.array);
                for (float vert : p.getVertices()) {
                    verticesArray.addChild(new JsonValue(vert));
                }
                s.addChild("vertices", verticesArray);

                JsonValue trianglesArray = new JsonValue(JsonValue.ValueType.array);
                for (short triangle : p.getIndices()) {
                    trianglesArray.addChild(new JsonValue(triangle));
                }
                s.addChild("triangles", trianglesArray);

                shapes.addChild(s);
            } else if (shape instanceof DrawableCircle) {
                DrawableCircle c = (DrawableCircle) shape;
                JsonValue s = new JsonValue(JsonValue.ValueType.object);
                s.addChild("type", new JsonValue("circle"));

                JsonValue colorArray = new JsonValue(JsonValue.ValueType.array);                colorArray.addChild(new JsonValue(c.getColor().r));
                colorArray.addChild(new JsonValue(c.getColor().g));
                colorArray.addChild(new JsonValue(c.getColor().b));
                colorArray.addChild(new JsonValue(c.getColor().a));
                s.addChild("color", colorArray);

                JsonValue paramsArray = new JsonValue(JsonValue.ValueType.array);
                paramsArray.addChild(new JsonValue(c.getCenter().x));
                paramsArray.addChild(new JsonValue(c.getCenter().y));
                paramsArray.addChild(new JsonValue(c.getRadius()));
                paramsArray.addChild(new JsonValue(c.getSegments()));
                s.addChild("params", paramsArray);

                shapes.addChild(s);
            }
        }

        json.addChild("shapes", shapes);

        if (saveAnim && animations.length > 0) {
            JsonValue jsonAnimations = new JsonValue(JsonValue.ValueType.array);
            for (Animation anim : animations)
                jsonAnimations.addChild(anim.toJson());
            json.addChild("animation", jsonAnimations);
        }

        return json;
    }


    public String toString() {
        String s = String.format(" [id:%s origin:%.1f %.1f]",
                id, localOrigin.x, localOrigin.y);
        return "ComplexShape@" + Integer.toHexString(hashCode()) + s;
    }
}
