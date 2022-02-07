package com.backend.qcmplus.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ParcoursBean  {

    private Long idParcours;

    private Long numTentative;

    private String nomQuestionnaire;
    private double note;
    private String dateFin;
    private String dateRealisation;
 }
