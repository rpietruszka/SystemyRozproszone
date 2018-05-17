import java.util.Random;

public enum Specialization {
    ELBOW,
    KNEE,
    HIP;

    public static Specialization getRandomSpecialization() {
        return values()[new Random().nextInt(values().length)];
    }
}
