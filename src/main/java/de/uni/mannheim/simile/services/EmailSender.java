/*
 * Copyright (c) 2017, Chair of Software Technology
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * •	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 * •	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 * •	Neither the name of the University Mannheim nor the names of its
 * 	contributors may be used to endorse or promote products derived from
 * 	this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

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
