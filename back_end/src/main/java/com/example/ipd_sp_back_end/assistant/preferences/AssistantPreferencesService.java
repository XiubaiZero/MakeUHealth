package com.example.ipd_sp_back_end.assistant.preferences;

import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class AssistantPreferencesService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    public AssistantPreferencesService(JdbcTemplate jdbc,TransactionTemplate transactions){this.jdbc=jdbc;this.transactions=transactions;}
    public record Preferences(boolean enterSendEnabled,long revision) { }
    private void ensure(int account) {
        jdbc.queryForObject("SELECT id FROM auth_user WHERE id=? FOR UPDATE",Integer.class,account);
        if (jdbc.queryForObject("SELECT COUNT(*) FROM assistant_preferences WHERE account_id=?",Integer.class,account)==0)
            jdbc.update("INSERT INTO assistant_preferences(account_id) VALUES(?)",account);
    }
    private Preferences read(int account) {
        return jdbc.queryForObject("SELECT enter_send_enabled,revision FROM assistant_preferences WHERE account_id=?",(r,n)->new Preferences(r.getBoolean(1),r.getLong(2)),account);
    }
    public Preferences get(int account){return transactions.execute(s->{ensure(account);return read(account);});}
    public Preferences update(int account,boolean enabled,long expected) {
        if(expected<0)throw new IllegalArgumentException("Preferences revision must be non-negative.");
        return transactions.execute(s->{
            ensure(account);
            if(read(account).revision()!=expected)throw new ResponseStatusException(HttpStatus.CONFLICT,"Chat preferences changed on another device. Refresh and try again.");
            jdbc.update("UPDATE assistant_preferences SET enter_send_enabled=?,revision=revision+1 WHERE account_id=?",enabled,account);
            return read(account);
        });
    }
}
