package org.netbeans.gradle.project.api.config;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a unique ID of a profile within a particular project. That is, this
 * ID does not need to be unique between two different projects.
 * <P>
 * Instances of this class are immutable and as such can be shared without any
 * further synchronization.
 */
public final class ProfileKey {
    /**
     * Defines the key for the default profile where a method takes a
     * {@code ProfileKey}.
     * <P>
     * <B>Note</B>: The value of this field is simply {@code null}, so you
     * cannot call any of its methods.
     */
    public static final ProfileKey DEFAULT_PROFILE = null;

    /**
     * Defines the key to access the default user private profile. It is rarely
     * appropriate to access this profile, instead you can use the group name
     * {@literal "private"} for your profile to be considered user specific.
     */
    public static final ProfileKey PRIVATE_PROFILE = new ProfileKey("private", "aux-config");

    private final String groupName;
    private final String fileName;

    /**
     * @deprecated You should either use {@link ProfileDef#getProfileKey()} or
     *   the {@link #fromProfileDef(ProfileDef) fromProfileDef} method.
     * <P>
     * Creates a {@code ProfileKey} from the
     * {@link ProfileDef#getGroupName() group name} and the
     * {@link ProfileDef#getFileName() file name} property of the given
     * {@code ProfileDef}.
     *
     * @param profileDef the {@code ProfileDef} whose properties are used to
     *   create the new {@code ProfileKey}. This argument cannot be
     *   {@code null}.
     */
    @Deprecated
    public ProfileKey(@Nonnull ProfileDef profileDef) {
        this(profileDef.getGroupName(), profileDef.getFileName());
    }

    /**
     * Creates a new {@code ProfileKey} instance with the specified properties.
     *
     * @param groupName the group (or namespace) of the profile. This argument
     *   must be a valid directory name or {@code null}. A {@code null} group
     *   means the default group where users create their profile.
     * @param fileName the name of the file into which the profile must be
     *   saved. The filename should not contain a directory path. This argument
     *   cannot be {@code null}.
     */
    public ProfileKey(@Nullable String groupName, @Nonnull String fileName) {
        ExceptionHelper.checkNotNullArgument(fileName, "fileName");

        this.groupName = groupName;
        this.fileName = fileName;
    }

    /**
     * Converts a {@code ProfileDef} to a {@code ProfileKey}. That is,
     * this method returns {@code profileDef.getProfileKey()} if the given
     * argument is not {@code null} and {@code null} if the passed argument is
     * {@code null}.
     *
     * @param profileDef the {@code ProfileDef} whose {@code ProfileKey} is to
     *   be retrieved. This argument can be {@code null}, in which case the
     *   return value is also {@code null}.
     * @return the {@code ProfileKey} representing the given {@code ProfileDef}.
     *   This method only returns {@code null} if the passed {@code ProfileDef}
     *   is {@code null}.
     */
    @Nullable
    public static ProfileKey fromProfileDef(@Nullable ProfileDef profileDef) {
        return profileDef != null ? profileDef.getProfileKey() : null;
    }

    /**
     * Returns the group (namespace) of this profile. The group name must be a
     * valid directory name.
     *
     * @return the group (namespace) of this profile. This method may return
     *   {@code null} which means the default namespace into which users create
     *   their own profiles.
     */
    @Nullable
    public String getGroupName() {
        return groupName;
    }

    /**
     * Returns the filename of this profile where the profile is to be stored.
     * This path should not contain a directory path but a simple filename.
     *
     * @return the filename of this profile where the profile is to be stored.
     *   This method never returns {@code null}.
     */
    @Nonnull
    public String getFileName() {
        return fileName;
    }

    /**
     * {@inheritDoc }
     *
     * @return a hash code compatible with the {@code equals} method
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.groupName);
        hash = 29 * hash + Objects.hashCode(this.fileName);
        return hash;
    }

    /**
     * Checks whether the given object is a {@code ProfileKey} defining the
     * same profile. Two {@code ProfileKey} instances are equal, if and only, if
     * both their {@link #getGroupName() group} and {@link #getFileName() filename}
     * properties are equal.
     * <P>
     * Note that although the check for equality is case-sensitive, you should
     * avoid using filenames (or groups) different only by case for different
     * profiles.
     *
     * @param obj the object to be compared against this profile. This argument
     *   can be {@code null}, in which case the return value is {@code null}.
     * @return {@code true} if the specified object defines the same profile as
     *   this {@code ProfileKey}, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final ProfileKey other = (ProfileKey)obj;
        return Objects.equals(this.groupName, other.groupName)
                && Objects.equals(this.fileName, other.fileName);
    }
}
