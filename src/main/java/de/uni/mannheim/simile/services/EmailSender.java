package de.uni.mannheim.simile.services;

import com.google.common.base.Strings;
import com.sparkpost.Client;
import com.sparkpost.exception.SparkPostException;
import com.sparkpost.model.AddressAttributes;
import com.sparkpost.model.RecipientAttributes;
import com.sparkpost.model.TemplateContentAttributes;
import com.sparkpost.model.TransmissionWithRecipientArray;
import com.sparkpost.model.responses.Response;
import com.sparkpost.resources.ResourceTransmissions;
import com.sparkpost.transport.RestConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class EmailSender {
	private static final Logger logger = LoggerFactory.getLogger(EmailSender.class);

	private static final String FROM_EMAIL = "postmaster@moteleclipse.cl";
	private static final String SPARKPOST_KEY = "20346025f3bce153a7ac0855785eec717ec41bfd";

	public void sendEmail(String[] recipients, String body) throws SparkPostException {
		TransmissionWithRecipientArray transmission = new TransmissionWithRecipientArray();

		// Populate Recipients
		List<RecipientAttributes> recipientArray = new ArrayList<>();
		for (String recipient : recipients) {
			RecipientAttributes recipientAttribs = new RecipientAttributes();
			recipientAttribs.setAddress(new AddressAttributes(recipient));
			recipientArray.add(recipientAttribs);
		}
		transmission.setRecipientArray(recipientArray);

		// Populate Substitution Data
		Map<String, Object> substitutionData = new HashMap<>();
		substitutionData.put("yourContent", "You can add substitution data too.");
		transmission.setSubstitutionData(substitutionData);

		logger.info("Body to send");
		logger.info(Strings.repeat("=", "Body to send".length()));
		logger.info(body);
		// Populate Email Body
		TemplateContentAttributes contentAttributes = new TemplateContentAttributes();
		contentAttributes.setFrom(new AddressAttributes(FROM_EMAIL));
		contentAttributes.setSubject("Feedback from Simile");
		contentAttributes.setText(body);
		contentAttributes.setHtml(body);
		transmission.setContentAttributes(contentAttributes);

		// Send the Email
		RestConnection connection = new RestConnection(new Client(SPARKPOST_KEY), "https://api.sparkpost.com/api/v1");
		Response response = ResourceTransmissions.create(connection, 0, transmission);

		logger.debug("Transmission Response: " + response);
	}

}
