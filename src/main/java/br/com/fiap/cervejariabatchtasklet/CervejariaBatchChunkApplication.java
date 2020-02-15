package br.com.fiap.cervejariabatchtasklet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;
import java.io.File;
import java.nio.file.Paths;

@SpringBootApplication
@EnableBatchProcessing
public class CervejariaBatchChunkApplication {

	//item reader -> item processor -> item writer

	Logger logger = LoggerFactory.getLogger(CervejariaBatchChunkApplication.class);

	@Bean
	public FlatFileItemReader<Pessoa> itemReader(@Value("${file.input}") Resource resource){
		return new FlatFileItemReaderBuilder<Pessoa>()
				.delimited().delimiter(";").names("nome", "cpf")
				.resource(resource)
				.targetType(Pessoa.class)
				.name("File Item Reader")
				.build();
	}

	@Bean
	public ItemProcessor<Pessoa, Pessoa> itemProcessor(){
		//new ItemProcessor
		return pessoa -> {
			pessoa.setNome(pessoa.getNome().toUpperCase());
			pessoa.setCpf(pessoa.getCpf()
					.replaceAll("\\.", "")
					.replace("-", ""));
			return pessoa;
		};
	}

	@Bean
	public ItemWriter<Pessoa> itemWriter(DataSource dataSource){
		return new JdbcBatchItemWriterBuilder<Pessoa>()
				.beanMapped()
				.dataSource(dataSource)
				.sql("INSERT INTO TB_PESSOA(NOME, CPF) VALUES (:nome, :cpf)")
				.build();
	}

	@Bean
	@Qualifier("stepchunk")
	public Step step(StepBuilderFactory stepBuilderFactory,
					 ItemReader<Pessoa> itemReader,
					 ItemProcessor<Pessoa, Pessoa> itemProcessor,
					 ItemWriter<Pessoa> itemWriter) {
		return stepBuilderFactory.get("step processar pessoa")
				.<Pessoa, Pessoa>chunk(2)
				.reader(itemReader)
				.processor(itemProcessor)
				.writer(itemWriter)
				.allowStartIfComplete(true)
				.build();
	}

	@Bean
	public Job job(JobBuilderFactory jobBuilderFactory, @Qualifier("stepchunk") Step step){
		return jobBuilderFactory.get("job processar pessoa")
				.start(step)
				.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(CervejariaBatchChunkApplication.class, args);
	}

}
