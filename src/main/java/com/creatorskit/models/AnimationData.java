package com.creatorskit.models;

import lombok.AllArgsConstructor;

import java.util.Arrays;

@AllArgsConstructor
public enum AnimationData
{
    POSE(new int[]
            {244, 808, 809, 813, 847, 1421, 1461, 1652, 1662, 1713, 1824, 1832, 1837,
            2061, 2065, 2074, 2148, 2316, 2561, 2911, 3040, 3175, 3296, 3677, 4193, 4646, 5160, 5246, 5253, 5363,
            5869, 6297, 6604, 6657, 6936, 7053, 7220, 7271, 7508, 7518, 7538, 8009, 8057, 8208, 8521, 9018, 9341,
            9460, 9494, 9814, 9857, 10032}),
    WALK(new int[]
            {247, 744, 762, 772, 819, 844, 1205, 1205, 1422, 1660, 1663, 1703, 1830, 1836, 2060, 2064, 2076,
            2317, 2562, 3039, 3177, 3293, 3415, 3680, 4194, 4226, 4682, 5164, 5245, 5250, 5364, 5867, 6607, 6658,
            6936, 6996, 7052, 7223, 7272, 7327, 7510, 7520, 7539, 7629, 8011, 8070, 8492, 8854, 9017, 9051, 9342,
            9461, 9849, 9859, 10076, 10170}),
    RUN(new int[]
            {248, 744, 762, 772, 824, 1210, 1427, 1440, 1661, 1664, 1707, 1836, 2077, 2322, 2563, 2847, 3178, 4228,
            5168, 5253, 5868, 6277, 6603, 6660, 6936, 6995, 7043, 7221, 7273, 7274, 7509, 7519, 7540, 7633, 7703,
            8016, 8070, 8492, 8853, 9019, 9051, 9346, 9459, 9850, 9860, 10077}),
    SHUFFLE_LEFT(new int[]
            {247, 745, 762, 772, 821, 844, 1207, 1424, 1468, 1660, 1663, 1706, 1836, 2060, 2064, 2076, 2319, 2562,
            3177, 3293, 3415, 3680, 4194, 5166, 5245, 5867, 6268, 6610, 6662, 6936, 6996, 7048, 7223, 7510, 7520,
            7631, 8013, 8070, 8492, 9021, 9052, 9343, 9861, 10055, 10076, 10170}),
    SHUFFLE_RIGHT(new int[]
            {247, 745, 762, 772, 822, 844, 1208, 1208, 1425, 1468, 1660, 1663, 1705, 1836, 2060, 2064, 2076, 2320,
            2562, 3177, 3293, 3415, 3680, 4194, 5167, 5245, 5867, 6275, 6609, 6663, 6936, 6996, 7047, 7223, 7510,
            7520, 7632, 8014, 8070, 8492, 9020, 9053, 9344, 9852, 9862, 10054, 10076, 10170}),
    BACKWARDS(new int[]
            {247, 745, 762, 772, 820, 844, 1206, 1423, 1468, 1660, 1663, 1704, 1830, 1836, 2060, 2064, 2076, 2562,
            3177, 3293, 3415, 3680, 4194, 4227, 5165, 5245, 5251, 5438, 5867, 6276, 6608, 6659, 6936, 6996, 7052,
            7223, 7327, 7510, 7520, 7539, 7630, 8012, 8070, 8492, 9017, 9054, 9345, 9461, 9859, 10076, 10170}),
    ROTATE(new int[]
            {745, 762, 765, 773, 823, 845, 1205, 1209,1426, 1468, 1702, 2321, 3177, 3415, 4194, 5161, 5252, 6297,
            6611, 6661, 6936, 6998, 7044, 8015, 8070, 8492, 9050, 9343, 9863, 10055})
    ;

    private final int[] animations;

    public static boolean matchAnimation(AnimationData animationData, int animationId)
    {
        return Arrays.stream(animationData.animations).anyMatch(n -> animationId == n);
    }

    public static PoseAnimation getPoseAnimation(int animationId)
    {
        if (matchAnimation(AnimationData.POSE, animationId))
            return PoseAnimation.POSE;

        if (matchAnimation(AnimationData.WALK, animationId))
            return PoseAnimation.WALK;

        if (matchAnimation(AnimationData.RUN, animationId))
            return PoseAnimation.RUN;

        if (matchAnimation(AnimationData.SHUFFLE_LEFT, animationId))
            return PoseAnimation.SHUFFLE_LEFT;

        if (matchAnimation(AnimationData.SHUFFLE_RIGHT, animationId))
            return PoseAnimation.SHUFFLE_RIGHT;

        if (matchAnimation(AnimationData.BACKWARDS, animationId))
            return PoseAnimation.BACKWARDS;

        if (matchAnimation(AnimationData.ROTATE, animationId))
            return PoseAnimation.ROTATE;

        return PoseAnimation.NONE;
    }
}
