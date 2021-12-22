package com.backend.qcmplus.service.impl;

import com.backend.qcmplus.model.Utilisateur;
import com.backend.qcmplus.repository.UtilisateurRepository;
import com.backend.qcmplus.service.UtilisateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UtilisateurServiceImpl implements UtilisateurService {

    @Autowired
    UtilisateurRepository userRepository;

    public List<Utilisateur> listAllUser() {
        return userRepository.findAllByIsadminFalse();
    }

    public Utilisateur saveUser(Utilisateur user) {
        return userRepository.save(user);
    }

    public Optional<Utilisateur> getUtilisateur(Long id) {
        return userRepository.findByidUtilisateur(id);
    }

    public void deleteUtilisateur(Long id) {
        userRepository.deleteByidUtilisateur(id);
    }

    @Override
    public Utilisateur getUtilisateurByLogin(String login) {
        return userRepository.findUtilisateurByLogin(login);
    }
}
