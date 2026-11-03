package Atos.SpringAPI.Controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import Atos.SpringAPI.Models.SheetPlayer;
import Atos.SpringAPI.Repositories.SheetPlayerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
@CrossOrigin(origins="http://localhost:4200")
@Tag(name = "Fichas", description = "CRUD de fichas de personagem")
@RestController
@RequestMapping("/sheetPlayer")
public class SheetPlayerController {
	
	@Autowired
	private SheetPlayerRepository sheetPlayerRepository;

	@Operation(summary = "Lista todas as fichas cadastradas")
	@GetMapping
	public List<SheetPlayer> listar(){
		
		return sheetPlayerRepository.findAll();
	}
	
	@Operation(summary = "Busca uma ficha pelo id")
	@GetMapping("/{id}")
	public Optional<SheetPlayer> listarFicha(@PathVariable Long id){
		try {
			return sheetPlayerRepository.findById(id);
		}
		catch(Exception e) {
			return null;	
		}
	}
	
	@Operation(summary = "Cadastra uma nova ficha")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SheetPlayer cadastrar(@RequestBody SheetPlayer sheetPlayer) {
		
		System.out.println(sheetPlayer.getNameJ());
		return sheetPlayerRepository.save(sheetPlayer);
	}
	
	@Operation(summary = "Substitui os dados de uma ficha existente")
	@PutMapping("/{id}")
	public ResponseEntity<SheetPlayer> updatePessoa(@RequestBody SheetPlayer sheetPlayer, @PathVariable Long id) {
		return sheetPlayerRepository.findById(id)
                .map(record -> {
                	/*Informações pessoais*/
                    record.setNameJ(sheetPlayer.getNameJ());
                    record.setNameC(sheetPlayer.getNameC());
                    record.setGender(sheetPlayer.getGender());
                    record.setAge(sheetPlayer.getAge());
                    record.setLifeA(sheetPlayer.getLifeA());
                    record.setLifeM(sheetPlayer.getLifeM());
                    record.setSanA(sheetPlayer.getSanA());
                    record.setSanM(sheetPlayer.getSanM());
                    /*Informações pessoais*/
                    /*Atributos*/
                    record.setSTR(sheetPlayer.getSTR());
                    record.setDEX(sheetPlayer.getDEX());
                    record.setCONS(sheetPlayer.getCONS());
                    record.setINT(sheetPlayer.getINT());
                    record.setCAR(sheetPlayer.getCAR());
                    record.setSAB(sheetPlayer.getSAB());
                    record.setTAM(sheetPlayer.getTAM());
                    record.setSOR(sheetPlayer.getSOR());
                    record.setPOD(sheetPlayer.getPOD());
                    /*Atributos*/
                    /*Pericias*/
                    record.setAtribute1(sheetPlayer.getAtribute1());
                    record.setAtribute2(sheetPlayer.getAtribute2());
                    record.setAtribute3(sheetPlayer.getAtribute3());
                    record.setAtribute4(sheetPlayer.getAtribute4());
                    record.setAtribute5(sheetPlayer.getAtribute5());
                    record.setAtribute6(sheetPlayer.getAtribute6());
                    record.setAtribute7(sheetPlayer.getAtribute7());
                    record.setAtribute8(sheetPlayer.getAtribute8());
                    record.setAtribute9(sheetPlayer.getAtribute9());
                    record.setAtribute10(sheetPlayer.getAtribute10());
                    record.setAtribute11(sheetPlayer.getAtribute11());
                    record.setAtribute12(sheetPlayer.getAtribute12());
                    record.setAtribute13(sheetPlayer.getAtribute13());
                    record.setAtribute14(sheetPlayer.getAtribute14());
                    record.setAtribute15(sheetPlayer.getAtribute15());
                    record.setAtribute16(sheetPlayer.getAtribute16());
                    record.setAtribute17(sheetPlayer.getAtribute17());
                    record.setAtribute18(sheetPlayer.getAtribute18());
                    record.setAtribute19(sheetPlayer.getAtribute19());
                    record.setAtribute20(sheetPlayer.getAtribute20());
                    record.setAtribute21(sheetPlayer.getAtribute21());
                    record.setAtribute22(sheetPlayer.getAtribute22());
                    record.setAtribute23(sheetPlayer.getAtribute23());
                    /*Pericias*/
                    SheetPlayer update = sheetPlayerRepository.save(record);
                    return ResponseEntity.ok().body(update);
                }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
		
	}
	
	
	@Operation(summary = "Exclui a ficha com o id informado")
	@DeleteMapping("/{id}")
	public void deletarSheetPlayer(@PathVariable Long id) {
		System.out.println(id);
		sheetPlayerRepository.deleteById(id);
	}
}
