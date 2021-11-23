package com.backend.qcmplus.model;

import java.io.Serializable;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ReponseUtilisateurQuestion")
@Getter
@Setter
@NoArgsConstructor
public class ReponseUtilisateurQuestion implements Serializable {

    @EmbeddedId
    private ReponseUtilisateurQuestionPk linkPk = new ReponseUtilisateurQuestionPk();
    private int reponse;

    public ReponseUtilisateurQuestion(int reponse) {
        this.reponse = reponse;
    }

}
