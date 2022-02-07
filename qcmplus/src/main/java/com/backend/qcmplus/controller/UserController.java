package com.backend.qcmplus.controller;


import com.backend.qcmplus.bean.QuestionnaireBean;
import com.backend.qcmplus.model.*;
import com.backend.qcmplus.service.QuestionService;
import com.backend.qcmplus.service.QuestionnaireService;
import com.backend.qcmplus.service.ReponseUtilisateurQuestionService;
import com.backend.qcmplus.service.UtilisateurService;
import com.backend.qcmplus.utils.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class references all the methods authorized for a user with the role of USER
 */
@RestController
@RequestMapping("/rest")
public class UserController {

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private QuestionnaireService surveyService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private ReponseUtilisateurQuestionService reponseUtilisateurQuestionService;

    @GetMapping("/login")
    Mono<String> hello() {
        return Mono.just("loged");
    }

    /**
     * This method retrieves all Questionnaire objects and transforms them into a list of beans.
     *
     * @return an HTTP Status OK when the method for retrieving all the surveys is successful.
     */
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/surveys")
    Mono<List<QuestionnaireBean>> findAllSurvey() {
        List<Questionnaire> questionnaires = surveyService.listAllSurvey();
        List<QuestionnaireBean> questionnaireBeanList = new ArrayList<>();
        for (Questionnaire questionnaire : questionnaires) {
            QuestionnaireBean questionnaireBean = new QuestionnaireBean();
            questionnaireBean.setIdQuestionnaire(questionnaire.getIdQuestionnaire());
            questionnaireBean.setNomQuestionnaire(questionnaire.getNomQuestionnaire());
            questionnaireBean.setDescription(questionnaire.getDescription());
            questionnaireBean.setPossedeParcours(false);
            if (reponseUtilisateurQuestionService.listAllReponsesFromLastTentative(questionnaire.getIdQuestionnaire()).size() > 0) {
                questionnaireBean.setPossedeParcours(true);
            }
            questionnaireBeanList.add(questionnaireBean);
        }
        return Mono.just(questionnaireBeanList);
    }

    /**
     * @return an HTTP Status OK when the method for retrieving all the questions is successful.
     */
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/question")
    Mono<List<Question>> findAllQuestion() {
        return Mono.just(questionService.listAllQuestion());
    }

    /**
     * @param id a Long object retrieved in the front referencing the survey's id
     * @return an HTTP Status OK when successfully retrieving the survey
     * @throws ObjectNotFoundException a customized exception when an object is not found
     */
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/survey/{id}")
    Mono<Questionnaire> findOneSurvey(@PathVariable Long id) throws ObjectNotFoundException {
        Optional<Questionnaire> foundSurvey = surveyService.findById(id);
        if (foundSurvey.isEmpty())
            throw new ObjectNotFoundException(id);
        foundSurvey.get().getListeQuestion().forEach(question -> question.getReponses().forEach(reponse -> reponse.setIsCorrect(null)));
        return Mono.just(foundSurvey.get());
    }

    /**
     * @param id a Long object retrieved in the front referencing the question's id
     * @return an HTTP Status OK when successfully retrieving the question
     * @throws ObjectNotFoundException a customized exception when an object is not found
     */
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/question/{id}")
    Mono<Question> findOneQuestion(@PathVariable Long id) throws ObjectNotFoundException {
        Optional<Question> foundQuestion = questionService.findById(id);
        if (foundQuestion.isEmpty())
            throw new ObjectNotFoundException(id);
        return Mono.just(foundQuestion.get());
    }

    /**
     * This method makes it possible to record the surveys carried out by the logged-in user.
     * These notes are stored in a list of beans of type Parcours in order to be sent to the frontend.
     *
     * @return a list of notes for each questionnaire associated with the connected user.
     * @throws Exception a general exception when no surveys associated with the connected user is found
     */
    @GetMapping("/parcours")
    public List<ParcoursBean> noteLastSurveys() throws Exception {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Questionnaire> liste = surveyService.listAllSurvey();
        List<ParcoursBean> notesQuestionnaireList = new ArrayList<>();
        if (principal instanceof UserDetails) {
            for (Questionnaire questionnaire : liste) {
                notesQuestionnaireList.add(noteOneSurvey(questionnaire.getIdQuestionnaire()));
            }
            if (notesQuestionnaireList.isEmpty()) {
                throw new Exception("pas de questionnaire");
            }
            return notesQuestionnaireList;
        }
        return new ArrayList<>();
    }

    /**
     * This method is used to score a questionnaire completed by the logged-in user.
     * This note is stored in a Parcours bean to be sent to the frontend.
     *
     * @param idQuestionnaire a Long object retrieved in the front referencing a survey's id
     * @return a Parcours bean with a note
     */
    @GetMapping("/questionnaire/{idQuestionnaire}/parcours")
    public ParcoursBean noteOneSurvey(@PathVariable Long idQuestionnaire) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        System.out.println(principal);
        List<ReponseUtilisateurQuestion> listeQuestionsRepondues = reponseUtilisateurQuestionService.listAllReponsesFromLastTentative(idQuestionnaire);
        double note = 0;
        ParcoursBean parcoursBean = new ParcoursBean();
        Optional<Questionnaire> questionnaire = surveyService.getSurvey(idQuestionnaire);
        parcoursBean.setNomQuestionnaire(questionnaire.get().getNomQuestionnaire());

        if (principal instanceof UserDetails) {
            for (ReponseUtilisateurQuestion reponseUtilisateurQuestion : listeQuestionsRepondues) {
                parcoursBean.setDateRealisation(reponseUtilisateurQuestion.getDateRealisation());
                parcoursBean.setDateFin(reponseUtilisateurQuestion.getDateFin());
                if (reponseUtilisateurQuestion.getReponse() == 1) {
                    note++;
                }
            }
            note = (note / listeQuestionsRepondues.size()) * 20;
            parcoursBean.setNote(note);
            return parcoursBean;
        }
        return new ParcoursBean();
    }

    /**
     * This method is used to note all the attempts of the connected user to a survey.
     *
     * @param idQuestionnaire a Long object retrieved in the front referencing a survey's id
     * @return a list of notes for each attempt associated with the connected user.
     */
    @GetMapping("/survey/{idQuestionnaire}/reponses")
    public List<ParcoursBean> noteAllAttempsToASurvey(@PathVariable Long idQuestionnaire) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        System.out.println(principal);
        List<ReponseUtilisateurQuestion> listeQuestionsRepondues = reponseUtilisateurQuestionService.listAllQuestionsFromOneQuestionnaire(idQuestionnaire);
        Map<Long, List<ReponseUtilisateurQuestion>> listeQuestionsReponduesGroupedByTentatives =
                listeQuestionsRepondues
                        .stream()
                        .collect(Collectors.groupingBy(w -> w.getLinkPk().getNumeroTentative()));

        List<ParcoursBean> parcoursBeanList = new ArrayList<>();
        Optional<Questionnaire> questionnaire = surveyService.getSurvey(idQuestionnaire);
        for (var entry : listeQuestionsReponduesGroupedByTentatives.entrySet()) {
            double note = 0;
            ParcoursBean parcoursBean = new ParcoursBean();
            parcoursBean.setNomQuestionnaire(questionnaire.get().getNomQuestionnaire());
            parcoursBean.setNumTentative(entry.getKey());
            if (principal instanceof UserDetails) {
                for (ReponseUtilisateurQuestion reponseUtilisateurQuestion : entry.getValue()) {
                    parcoursBean.setDateRealisation(reponseUtilisateurQuestion.getDateRealisation());
                    parcoursBean.setDateFin(reponseUtilisateurQuestion.getDateFin());
                    if (reponseUtilisateurQuestion.getReponse() == 1) {
                        note++;
                    }
                }
                note = (note / entry.getValue().size()) * 20;
                parcoursBean.setNote(note);
                parcoursBeanList.add(parcoursBean);
            }
        }

        return parcoursBeanList;
    }

    @Transactional
    @PostMapping("/questionnaire/repondre")
    void saveNewUser(@RequestBody QuestionnaireBean questionnaire) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        Utilisateur utilisateur = utilisateurService.getUtilisateurByLogin(username);
        surveyService.addAnswer(questionnaire, utilisateur);
    }
}

