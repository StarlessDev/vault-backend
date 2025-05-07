package dev.starless.hosting.objects;

import dev.starless.hosting.objects.session.UserInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uniqueEmail", columnNames = { "email" }),
        @UniqueConstraint(name = "uniqueUsernames", columnNames = { "username" })
})
@Entity
public class ServiceUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String username;
    private String email;

    private String hashedPassword;

    public UserInfo toUserInfo() {
        return new UserInfo(id, username);
    }
}
