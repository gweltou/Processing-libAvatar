package gwel.game.anim;

import gwel.game.graphics.ComplexShape2;


public class Posture2 {
    private String name;
    private float duration;
    private TimeFunction[] timeFunctions;
    private PostureTree postureTree;

    /*
    private final Affine2 transform, oldTransform, nextTransform;

    public Posture() {
        transform = new Affine2();
        oldTransform = new Affine2();
        nextTransform = new Affine2();
    }*/


    public void setName(String name) { this.name = name; }

    public void setDuration(float duration) { this.duration = duration; }

    public void setPostureTree(PostureTree tree) { this.postureTree = tree; }

    /**
     * Updates the TimeFunctions used by this posture and
     * Returns an array of ComplexShape instances and their corresponding local animation matrices
     *
     * @param dtime
     */
    public void update(float dtime) {
        for (TimeFunction fn : timeFunctions)
            fn.update(dtime);
        postureTree.updateTransform();
     }


     public void reset() {
         for (TimeFunction fn : timeFunctions)
             fn.reset();
         postureTree.updateTransform();
     }

    /**
     * Used by SGAnimator editor only
     *
     * @param shape
     * @param idx
     * @return
     */
    public Animation2 getAnimation(ComplexShape2 shape, int idx) {
        PostureTree pt = postureTree.findByShape(shape);
        if (pt == null)
            return null;
        return pt.getAnimations().get(idx);
    }


    /**
     * Used by SGAnimator editor only
     *
     * @param shape
     * @param idx
     * @param anim
     */
    public void setAnimation(ComplexShape2 shape, int idx, Animation2 anim) {
        PostureTree pt = postureTree.findByShape(shape);
        assert (pt != null) : String.format("Shape %s not found in PostureTree", shape);
        pt.getAnimations().set(idx, anim);
    }


    /**
     * Used by SGAnimator editor only
     *
     * @param shape
     * @param anim New animation to add to the shape
     */
    public void addAnimation(ComplexShape2 shape, Animation2 anim) {
        PostureTree pt = postureTree.findByShape(shape);
        assert (pt != null) : String.format("Shape %s not found in PostureTree", shape);
        pt.getAnimations().add(anim);
    }


    /**
     * Used by SGAnimator editor only
     *
     * @param shape
     * @param idx
     */
    public void removeAnimation(ComplexShape2 shape, int idx) {
        PostureTree pt = postureTree.findByShape(shape);
        assert (pt != null) : String.format("Shape %s not found in PostureTree", shape);
        pt.getAnimations().remove(idx);
    }

    public String toString() {
        String s = String.format(" [name: %s duration: %.1f]", name, duration);
        return super.toString() + s;
    }
}