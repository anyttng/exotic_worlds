package com.exoticworlds.engine.net;

final class CameraSide {
    private boolean secondCopy;

    private boolean answersCamera;

    boolean secondCopy() {
        return secondCopy;
    }

    void markSecondCopy() {
        secondCopy = true;
    }

    boolean answersCamera() {
        return answersCamera;
    }

    void answerCamera() {
        answersCamera = true;
    }

    void reset() {
        secondCopy = false;
        answersCamera = false;
    }
}
