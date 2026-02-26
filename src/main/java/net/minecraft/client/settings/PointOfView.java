package net.minecraft.client.settings;

public enum PointOfView
{
    FIRST_PERSON(true, false),
    THIRD_PERSON_BACK(false, false),
    THIRD_PERSON_FRONT(false, true);

    private static final PointOfView[] field_243189_d = values();
    public boolean firstPerson;
    public boolean thirdPersonFront;

    private PointOfView(boolean p_i242049_3_, boolean p_i242049_4_)
    {
        this.firstPerson = p_i242049_3_;
        this.thirdPersonFront = p_i242049_4_;
    }

    public boolean func_243192_a()
    {
        return this.firstPerson;
    }

    public boolean func_243193_b()
    {
        return this.thirdPersonFront;
    }

    public PointOfView func_243194_c()
    {
        return field_243189_d[(this.ordinal() + 1) % field_243189_d.length];
    }
}
