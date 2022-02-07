package com.backend.qcmplus.service;

import com.backend.qcmplus.model.ReponseUtilisateurQuestion;
import com.backend.qcmplus.model.Utilisateur;
import com.backend.qcmplus.repository.QuestionnaireRepository;
import com.backend.qcmplus.repository.ReponseUtilisateurQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReponseUtilisateurQuestionService {

    @Autowired
    ReponseUtilisateurQuestionRepository repository;
    @Autowired
    QuestionnaireRepository questionnaireRepository;
    @Autowired
    UtilisateurService utilisateurService;

    //TODO: Find a better place for this method
    public Utilisateur getCurrentUtilisateur() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails)principal).getUsername();
        } else {
           throw new UsernameNotFoundException("username not found");
        }
        return utilisateurService.getUtilisateurByLogin(username);
    }

    public List<ReponseUtilisateurQuestion> listAllQuestions(){
        return repository.findAll();
    }

    public List<ReponseUtilisateurQuestion> listAllQuestionsByUtilisateur(Long idUser){
        return repository.findAllByLinkPk_Utilisateur_IdUtilisateurOrderByLinkPkNumeroTentative(idUser);
    }

    public List<ReponseUtilisateurQuestion> listAllQuestionsFromOneQuestionnaire(Long idQuestionnaire) {
        Utilisateur utilisateur = getCurrentUtilisateur();
        List<ReponseUtilisateurQuestion> listeQuestionsRepondues =listAllQuestionsByUtilisateur(utilisateur.getIdUtilisateur());
        listeQuestionsRepondues.removeIf(questionRepondue -> !questionRepondue.getLinkPk().getQuestion().getQuestionnaire().getIdQuestionnaire().equals(idQuestionnaire));
    return listeQuestionsRepondues;
    }

    public List<ReponseUtilisateurQuestion> listAllReponsesFromLastTentative(Long idQuestionnaire){
        List<ReponseUtilisateurQuestion> listeQuestions = listAllQuestionsFromOneQuestionnaire(idQuestionnaire);
        long numeroMax = listeQuestions
                .stream()
                .mapToLong(v -> v.getLinkPk().getNumeroTentative())
                .max().orElse(0);

        return listeQuestions
                .stream()
                .filter(reponseUtilisateurQuestion -> reponseUtilisateurQuestion.getLinkPk().getNumeroTentative()==numeroMax)
                .collect(Collectors.toList());
    }

}
