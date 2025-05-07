package dev.starless.hosting.objects.bodies;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class RegisterBody extends LoginBody {

    private final String username;

    public RegisterBody(String username, String email, String password) {
        super(email, password);

        this.username = username;
    }
}
