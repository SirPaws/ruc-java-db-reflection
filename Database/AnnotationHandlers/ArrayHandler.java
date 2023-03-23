package Database.AnnotationHandlers;

import Database.Annotations.Array;

import java.lang.annotation.Annotation;

public class ArrayHandler extends ReferenceHandler {

    @Override
    public void annotate(StringBuilder builder, Annotation self_) {
        if (!(self_ instanceof Array self)) throw new Error();
        annotateReferenceLike(builder, self_);
    }

    @Override
    public void lateAnnotate(StringBuilder builder, Annotation self_) {
        if (!(self_ instanceof Array self)) throw new Error();
        lateAnnotateReferenceLike(builder, self.value());
    }
}
