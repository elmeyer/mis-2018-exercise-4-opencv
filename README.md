Marques Everett Mondliwethu Mthunzi - 119119

Lars Meyer - 114719

A compiled apk of the app can be found in app/build/outputs/apk/debug/app-debug.apk

The red nose placement proceeds as follows:

1. Detect face(s) using `haarcascade_frontalface_default.xml`
2. Define ROI on face, detect eyes using `haarcascade_eye.xml`
3. If two eyes were detected:
    1. Draw a line between the bottom right corner of the left eye's ROI to the
       bottom left corner of the right eye's ROI
    2. Calculate the orthogonal line and draw it from the midpoint of the line
       between the two eyes to the tip of the nose (approx. half the length of
       the line between the eyes)
    3. Draw a filled red circle with center = endpoint of orthogonal line and
       diameter approx `0.12 * face.width`

The diameter of the red circle was determined based on the face's width because
this proved to be more reliable than the initial approach of determining it
based on the distance between the eye ROIs. The concrete value was found by
educated guess and refined by trial and error.
