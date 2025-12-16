package suy.sk8.coach.config;

import com.github.lianjiatech.retrofit.spring.boot.core.RetrofitScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@RetrofitScan("suy.sk8.coach.api")
public class RetrofitConfig {
}