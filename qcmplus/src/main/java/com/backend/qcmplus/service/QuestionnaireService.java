package com.backend.qcmplus.service;

import com.backend.qcmplus.bean.QuestionnaireBean;
import com.backend.qcmplus.model.*;
import com.backend.qcmplus.repository.QuestionRepository;
import com.backend.qcmplus.repository.QuestionnaireRepository;
import com.backend.qcmplus.repository.ReponseUtilisateurQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional
public class QuestionnaireService {

    @Autowired
    private QuestionnaireRepository questionnaireRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ReponseUtilisateurQuestionRepository reponseUtilisateurQuestionRepository;

    public List<Questionnaire> listAllSurvey() {
        return questionnaireRepository.findAll();
    }

    public Questionnaire saveSurvey(Questionnaire question) {
        return questionnaireRepository.save(question);
    }

    public Optional<Questionnaire> getSurvey(Long id) {
        return questionnaireRepository.findById(id);
    }

    public void deleteSurvey(Long id) {
        questionnaireRepository.deleteById(id);
    }

    public Optional<Questionnaire> findById(Long id) {
        return questionnaireRepository.findById(id);
    }

    public List<Question> listAllQuestion(Long id) {
        Optional<Questionnaire> find = questionnaireRepository.findById(id);
        if (!find.isEmpty()) {
            return find.get().getListeQuestion();
        }
        return new ArrayList<Question>();
    }

    public void addAnswer(QuestionnaireBean questionnaire, Utilisateur utilisateur) {
        Optional<Questionnaire> questionnaireBdd = questionnaireRepository.findById(questionnaire.getIdQuestionnaire());
        if (questionnaireBdd.isPresent()){
            for (Question question : questionnaire.getListeQuestion()){
                boolean isAnswered = false;
                boolean isCorrect = true;
                Optional<Question> questionBdd = questionnaireBdd.get().getListeQuestion().stream().filter(q -> q.getIdQuestion().equals(question.getIdQuestion())).findFirst();
                if (questionBdd.isPresent()){
                    List<Reponse> reponsesBdd = questionBdd.get().getReponses();
                    for (Reponse reponse : question.getReponses()){
                        if (reponse.getIsCorrect() != null){
                            isAnswered = true;
                            Long nbBonnesReponsesBdd = reponsesBdd.stream().filter(Reponse::getIsCorrect).count();
                            Long nbBonnesReponsesUser = question.getReponses().stream().filter(reponse1 -> reponse1.getIsCorrect() != null && reponse1.getIsCorrect()).count();
                            if (!nbBonnesReponsesBdd.equals(nbBonnesReponsesUser)){
                                isCorrect = false;
                                break;
                            }
                            for (Reponse reponseBdd : reponsesBdd){
                                if (reponse.getIdReponse().equals(reponseBdd.getIdReponse()) && !reponseBdd.getIsCorrect()){
                                    isCorrect = false;
                                    break;
                                }
                            }
                        }
                    }
                }
                ReponseUtilisateurQuestion reponseUtilisateurQuestion = new ReponseUtilisateurQuestion();
                reponseUtilisateurQuestion.setDateFin(questionnaire.getDateFin());
                reponseUtilisateurQuestion.setDateRealisation(questionnaire.getDateFormulaire());
                ReponseUtilisateurQuestionPk reponseUtilisateurQuestionPk = new ReponseUtilisateurQuestionPk();
                reponseUtilisateurQuestionPk.setQuestion(question);
                reponseUtilisateurQuestionPk.setUtilisateur(utilisateur);
                if (!isAnswered){
                    reponseUtilisateurQuestion.setReponse(0);
                } else if (!isCorrect){
                    reponseUtilisateurQuestion.setReponse(-1);
                } else {
                    reponseUtilisateurQuestion.setReponse(1);
                }
                List<ReponseUtilisateurQuestion> reponseUtilisateurQuestionBdd = reponseUtilisateurQuestionRepository.findAllByLinkPk_Utilisateur_IdUtilisateurAndLinkPk_Question_IdQuestionOrderByLinkPkNumeroTentative(utilisateur.getIdUtilisateur(), question.getIdQuestion());
                if (reponseUtilisateurQuestionBdd.size() == 0){
                    reponseUtilisateurQuestionPk.setNumeroTentative(1L);
                } else {
                    long numeroMax = reponseUtilisateurQuestionBdd
                            .stream()
                            .mapToLong(v -> v.getLinkPk().getNumeroTentative())
                            .max().orElseThrow(NoSuchElementException::new);
                    reponseUtilisateurQuestionPk.setNumeroTentative(numeroMax + 1L);
                }
                reponseUtilisateurQuestion.setLinkPk(reponseUtilisateurQuestionPk);
                reponseUtilisateurQuestionRepository.save(reponseUtilisateurQuestion);
            }
        }
    }
}
