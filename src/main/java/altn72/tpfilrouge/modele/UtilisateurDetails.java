package altn72.tpfilrouge.modele;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class UtilisateurDetails extends User {

    public static final String PROFIL_RESPONSABLE_EXPLOITATION = "RESPONSABLE_EXPLOITATION";

    private final String prenom;
    private final String profil;

    public UtilisateurDetails(String username, String password, Collection<? extends GrantedAuthority> authorities, String prenom, String profil) {
        super(username, password, authorities);
        this.prenom = prenom;
        this.profil = profil;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getProfil() {
        return profil;
    }

    public boolean isResponsableExploitation() {
        return PROFIL_RESPONSABLE_EXPLOITATION.equals(profil);
    }
}