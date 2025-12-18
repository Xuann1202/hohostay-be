package tw.com.ispan.eeit.ho_back.inventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import tw.com.ispan.eeit.ho_back.inventory.dto.HotelQueryDto;

@RestController
public class InventoryController {
    @Autowired
    InventoryService inventoryService;

    @GetMapping("/api/searchHotel")
    public ResponseEntity<?> searchHotel(@Valid HotelQueryDto query, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            List<FieldError> fieldErrors = bindingResult.getFieldErrors();
            Map<String, String> errors = new HashMap<>();
            for (FieldError fieldError : fieldErrors) {
                String field = fieldError.getField();
                String errorMessage = fieldError.getDefaultMessage();
                errors.put(field, errorMessage);
            }
            return ResponseEntity.status((HttpStatus.BAD_REQUEST)).body(errors);
        }
        query.setKeyword(query.getKeyword().replace('台', '臺'));
        Map<String, Object> results = inventoryService.searchHotel(query);
        System.out.println(results);
        return ResponseEntity.status(HttpStatus.OK).body(results);
    }

}
