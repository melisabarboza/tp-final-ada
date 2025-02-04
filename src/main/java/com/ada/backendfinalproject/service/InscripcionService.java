package com.ada.backendfinalproject.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ada.backendfinalproject.entity.Curso;
import com.ada.backendfinalproject.entity.Inscripcion;
import com.ada.backendfinalproject.entity.Participante;
import com.ada.backendfinalproject.entity.enums.EstadoInscripcion;
import com.ada.backendfinalproject.repository.InscripcionRepository;
import com.ada.backendfinalproject.solicitudes.FormCambiarEstadoInscripcion;
import com.ada.backendfinalproject.solicitudes.FormFinalizarInscripcion;
import com.ada.backendfinalproject.solicitudes.FormNewInscripcion;

@Service
public class InscripcionService {

	@Autowired
	InscripcionRepository inscripcionRepository;

	@Autowired
	ParticipanteService participanteService;

	@Autowired
	CursoService cursoService;

	public Inscripcion addNewInscripcion(FormNewInscripcion solicitud, String nombreUsuarioLogueado) throws Exception {

		// TODO: Validar que el curso tenga cupos disponibles
		Optional<Curso> cursoOpt = cursoService.getCursoById(solicitud.getIdCurso());

		if (cursoOpt.isPresent()) {
			Integer cantidadParticipantes = cursoOpt.get().getNumeroParticipantes();
			if (cantidadParticipantes <= 0) {
				throw new Exception("El curso solicitado ya no posee vacantes disponibles");
			}

		} else {
			throw new Exception("IdCurso invalido");
		}

		Optional<Participante> optParticipante = participanteService.getParticipanteByUsuario(nombreUsuarioLogueado);
		if (!optParticipante.isPresent())
			throw new Exception("Participante inexistente");

		Inscripcion inscripcion = new Inscripcion(0, cursoOpt.get(), optParticipante.get(), solicitud.getSolicitaBeca(),
				null, EstadoInscripcion.PENDIENTE.name());

		if (solicitud.getSolicitaBeca()) {

			if (cursoOpt.get().getBecasDisponibles() < 1) {
				throw new Exception("El curso no posee becas disponibles");
			}

			if (optParticipante.get().DatosSocioEconomicosCargados()) {
				return inscripcionRepository.save(inscripcion);
			} else
				throw new Exception("Datos socioeconomicos no cargados");

		} else {
			inscripcion.setEstadoInscripcion(EstadoInscripcion.APROBADO.name());
			cursoService.restarVacante(solicitud.getIdCurso());
			return inscripcionRepository.save(inscripcion);
		}

	}

	public Iterable<Inscripcion> getInscripcionPorIdParticipante(Integer idParticipante) {
		Iterable<Inscripcion> itInscripciones = inscripcionRepository.findByParticipanteId(idParticipante);
		return itInscripciones;
	}

	public Iterable<Inscripcion> getInscripcionAprobadasPorIdParticipante(Integer idParticipante) {
		Iterable<Inscripcion> itInscripciones = inscripcionRepository
				.findByParticipanteIdAndEstadoInscripcion(idParticipante, EstadoInscripcion.APROBADO.name());
		return itInscripciones;
	}

	public Iterable<Inscripcion> getInscripcionFinalizadaPorIdParticipante(Integer idParticipante) {
		Iterable<Inscripcion> itInscripciones = inscripcionRepository
				.findByParticipanteIdAndEstadoInscripcion(idParticipante, EstadoInscripcion.FINALIZADO.name());
		return itInscripciones;
	}

	public Inscripcion cambiarEstado(FormCambiarEstadoInscripcion solicitud) throws Exception {

		Optional<Inscripcion> inscripcion = inscripcionRepository.findById(solicitud.getIdInscripcion());

		if (!inscripcion.isPresent()) {
			throw new Exception("Inscripcion no encontrada");
		}

		if (!inscripcion.get().getEstadoInscripcion().equals(EstadoInscripcion.PENDIENTE.name())) {
			throw new Exception("El estado de la inscripción ya fue modificado anteriormente");
		}

		if (solicitud.getPorcentajeBeca() != 50 && solicitud.getPorcentajeBeca() != 75
				&& solicitud.getPorcentajeBeca() != 100) {
			throw new Exception("Porcentaje beca incorrecto");
		}
		if (solicitud.getEstado() == EstadoInscripcion.PENDIENTE) {
			throw new Exception("El estado de inscripcion es invalido");
		}

		inscripcion.get().setEstadoInscripcion(solicitud.getEstado().name());

		if (solicitud.getEstado() == EstadoInscripcion.APROBADO) {
			inscripcion.get().setPorcentajeBeca(solicitud.getPorcentajeBeca());
			cursoService.restarVacanteYBeca(inscripcion.get().getCurso().getId());
		}

		return inscripcionRepository.save(inscripcion.get());
	}

	public Inscripcion finalizar(FormFinalizarInscripcion solicitud) throws Exception {
		Optional<Inscripcion> inscripcion = inscripcionRepository.findById(solicitud.getIdInscripcion());
		if (!inscripcion.isPresent()) {
			throw new Exception("Inscripcion no encontrada");
		}
		if (inscripcion.get().getEstadoInscripcion() != EstadoInscripcion.APROBADO.name()) {
			throw new Exception("El estado actual de la inscripcion no es APROBADO");
		}
		inscripcion.get().setEstadoInscripcion(EstadoInscripcion.FINALIZADO.name());
		return inscripcionRepository.save(inscripcion.get());

	}
}