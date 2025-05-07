package dev.starless.hosting.objects.bodies;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public class LoginBody {

    private final String email;
    private final String password;

}
