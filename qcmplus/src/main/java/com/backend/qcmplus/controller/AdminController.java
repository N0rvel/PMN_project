package com.backend.qcmplus.controller;

import com.backend.qcmplus.model.Question;
import com.backend.qcmplus.model.Questionnaire;
import com.backend.qcmplus.model.Reponse;
import com.backend.qcmplus.model.Utilisateur;
import com.backend.qcmplus.repository.ReponseRepository;
import com.backend.qcmplus.service.QuestionService;
import com.backend.qcmplus.service.QuestionnaireService;
import com.backend.qcmplus.service.UtilisateurService;
import com.backend.qcmplus.utils.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.transaction.Transactional;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * This class references all the methods authorized for a user with the role of ADMIN.
 */
@RestController
@RequestMapping("/admin/rest")
public class AdminController {

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private QuestionnaireService surveyService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private ReponseRepository reponseRepository;

    /**
     * @param newUser a User object with attributes given by the front
     * @return an HTTP Status CREATED if the creation of a new user is successful
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/users")
    Mono<Utilisateur> saveNewUser(@RequestBody Utilisateur newUser) {
        String pwd = newUser.getPassword();
        String encryptPwd = passwordEncoder.encode(pwd);
        newUser.setPassword(encryptPwd);
        return Mono.just(utilisateurService.saveUser(newUser));
    }

    /**
     * @param newUser a User object with attributes given by the front
     * @return an HTTP Status CREATED if the creation of a new user is successful
     * @throws Exception a general exception giving the information that this login already exists
     */
    @PostMapping("/user")
    HttpStatus newUser(@RequestBody Utilisateur newUser) throws Exception {
        if ("".equals(newUser.getPassword()) || newUser.getPassword() == null) {
            newUser.setPassword(utilisateurService.getUtilisateur(newUser.getIdUtilisateur()).get().getPassword());
        } else {
            String pwd = newUser.getPassword();
            String encryptPwd = passwordEncoder.encode(pwd);
            newUser.setPassword(encryptPwd);
        }
        if (newUser.getIdUtilisateur() == null) {
            Utilisateur utilisateur = utilisateurService.getUtilisateurByLogin(newUser.getLogin());
            if (utilisateur != null) {
                throw new Exception("Ce login existe déjà");
            }
        }
        utilisateurService.saveUser(newUser);
        return HttpStatus.CREATED;

    }

    /**
     * @return a list of all the users presents in the database
     */
    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/users")
    Mono<List<Utilisateur>> findAll() {
        return Mono.just(utilisateurService.listAllUser());
    }

    /**
     * @param id a Long object referencing the id of a user retrieved by the front-end
     * @return the user associated with the id passed in parameters
     * @throws ObjectNotFoundException a specific exception thrown when the user's id is not present in the database
     */
    @GetMapping("/users/{id}")
    Mono<Utilisateur> findOne(@PathVariable Long id) throws ObjectNotFoundException {
        Optional<Utilisateur> foundUser = utilisateurService.getUtilisateur(id);
        if (foundUser.isEmpty())
            throw new ObjectNotFoundException(id);
        return Mono.just(foundUser.get());
    }

    /**
     * @param newUser a User object with attributes given by the front
     * @param id      a Long object referencing the id of a user retrieved by the front-end
     * @return the user associated with the id passed in parameters with its updated attributes
     */
    @PutMapping("/users/{id}")
    Mono<Utilisateur> saveOrUpdate(@RequestBody Utilisateur newUser, @PathVariable Long id) {
        Optional<Utilisateur> foundUser = utilisateurService.getUtilisateur(id);

        return foundUser.map(x -> {
            x.setLogin(newUser.getLogin());
            x.setMail(newUser.getMail());
            x.setNom(newUser.getNom());
            x.setPassword(newUser.getPassword());
            x.setPrenom(newUser.getPrenom());
            x.setSociete(newUser.getSociete());

            return Mono.just(utilisateurService.saveUser(x));
        }).orElseGet(() -> {
            newUser.setIdUtilisateur(id);
            return Mono.just(utilisateurService.saveUser(newUser));
        });
    }

    /**
     * @param id a Long object referencing the id of a user retrieved by the front-end
     */
    @GetMapping("/user/{id}")
    void deleteUtilisateur(@PathVariable Long id) {
        utilisateurService.deleteUtilisateur(id);
    }

    /**
     * @param newSurvey a Survey object with attributes given by the front
     * @return an HTTP Status CREATED if the creation of a new survey is successful
     */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/survey")
    Mono<Questionnaire> newSurvey(@RequestBody Questionnaire newSurvey) {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");// dd/MM/yyyy
        Date now = new Date();
        String strDate = sdfDate.format(now);
        newSurvey.setDateCreation(strDate);
        return Mono.just(surveyService.saveSurvey(newSurvey));
    }

    /**
     * @return an HTTP Status OK when the method of getting all the surveys is successful
     */
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/surveys")
    Mono<List<Questionnaire>> findAllSurveys() {
        return Mono.just((surveyService.listAllSurvey()));
    }

    /**
     * @param newQuestion a Question object with attributes given by the front
     * @return an HTTP Status CREATED if the creation of a new question is successful
     */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/question")
    @Transactional
    Mono<Question> newQuestion(@RequestBody Question newQuestion) {
        Question question = questionService.saveQuestion(newQuestion);
        for (Reponse reponse : question.getReponses()) {
            reponse.setQuestion(question);
        }
        return Mono.just(questionService.saveQuestion(question));
    }

    /**
     * @param newQuestion a Question object with attributes given by the front
     * @param id          a Long object retrieved in the front referencing the question's id
     * @return an HTTP Status OK when the method of updating the question is successful
     */
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/question/{id}")
    Mono<Question> saveOrUpdateQuestion(@RequestBody Question newQuestion, @PathVariable Long id) {
        Optional<Question> foundQuestion = questionService.findById(id);

        return foundQuestion.map(x -> {
            x.setIntitule(newQuestion.getIntitule());
            x.setQuestionnaire(newQuestion.getQuestionnaire());
            x.setReponses(newQuestion.getReponses());

            return Mono.just(questionService.saveQuestion(x));
        }).orElseGet(() -> {
            newQuestion.setIdQuestion(id);
            return Mono.just(questionService.saveQuestion(newQuestion));
        });
    }

    /**
     * @param id a Long object retrieved in the front referencing the question's id
     * @return an HTTP Status OK when the method of getting all questions of a survey is successful
     */
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/survey/{id}/question")
    Mono<List<Question>> getAllQuestion(@PathVariable long id) {
        return Mono.just(surveyService.listAllQuestion(id));
    }

    /**
     * @param newSurvey a Survey object with attributes given by the front
     * @param id a Long object retrieved in the front referencing the survey's id
     * @return an HTTP Status OK when the method of updating the survey is successful
     */
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/survey/{id}")
    Mono<Questionnaire> saveOrUpdateSurvey(@RequestBody Questionnaire newSurvey, @PathVariable Long id) {
        Optional<Questionnaire> foundSurvey = surveyService.findById(id);

        return foundSurvey.map(x -> {
            x.setDescription(newSurvey.getDescription());
            x.setDateCreation(newSurvey.getDateCreation());
            x.setNomQuestionnaire(newSurvey.getNomQuestionnaire());
            x.setListeQuestion(newSurvey.getListeQuestion());

            return Mono.just(surveyService.saveSurvey(x));
        }).orElseGet(() -> {
            newSurvey.setIdQuestionnaire(id);
            return Mono.just(surveyService.saveSurvey(newSurvey));
        });
    }

    /**
     * @param id a Long object retrieved in the front referencing the survey's id
     */
    @GetMapping("/survey/{id}")
    void deleteSurvey(@PathVariable Long id) {
        surveyService.deleteSurvey(id);
    }

    /**
     * @param id a Long object retrieved in the front referencing the question's id
     */
    @GetMapping("/question/{id}")
    void deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
    }
}
