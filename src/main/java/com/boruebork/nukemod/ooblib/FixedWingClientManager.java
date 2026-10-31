package com.boruebork.nukemod.ooblib;

import com.boruebork.nukemod.aircraft.AbstractFixedWingAircraft;

public class FixedWingClientManager {

    public static class PilotingClientState{
        public static AbstractFixedWingAircraft aircraft;
        public static float x;
        public static float z;
        public static boolean up;
        public static boolean down;
        public static float xRot;
        public static float xRotO;
        public static float yRot;
        public static float yRotO;
        public static float movementYRot;
        public static int currentPayloadMode = 0;
        public static float roll;
        public static float rollO;
        public static float rollW;
        public static float yW;
        public static float xW;
        public static float cameraYaw;
        public static float cameraPitch;
        public static float cameraYawO;
        public static float cameraPitchO;
    }
}
