<?xml version="1.0" encoding="UTF-8" ?>
<!-- 


    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/
	Developed by DSpace @ Lyncode <dspace@lyncode.com>
	
	> http://www.openarchives.org/OAI/2.0/oai_dc.xsd

 -->
<xsl:stylesheet 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:doc="http://www.lyncode.com/xoai"
	version="1.0">
	<xsl:output omit-xml-declaration="yes" method="xml" indent="yes" />

	<xsl:variable name="UdeM" select="doc:metadata/doc:element[@name = 'UdeM']"/>
	<xsl:variable name="dcterms" select="doc:metadata/doc:element[@name = 'dcterms']"/>
	<xsl:variable name="dc" select="doc:metadata/doc:element[@name = 'dc']"/>
	<xsl:variable name="oaire" select="doc:metadata/doc:element[@name = 'oaire']"/>
	<xsl:variable name="bundles" select="doc:metadata/doc:element[@name = 'bundles']"/>
		
	<xsl:template match="/">
		<oai_dc:dc xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" 
			xmlns:dc="http://purl.org/dc/elements/1.1/" 
			xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
			xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" 
			xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/
			http://www.openarchives.org/OAI/2.0/oai_dc.xsd
			http://www.w3.org/1999/02/22-rdf-syntax-ns#
			http://www.openarchives.org/OAI/2.0/rdf.xsd ">
			
			<!-- dc.title -->
			<xsl:for-each select="$dc/doc:element[@name='title']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:title><xsl:value-of select="." /></dc:title>
			</xsl:for-each>
			<!-- dc.title.* -->
			<xsl:for-each select="$dc/doc:element[@name='title']/doc:element/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:title><xsl:value-of select="." /></dc:title>
			</xsl:for-each>
			<!-- dc.creator -->
			<xsl:for-each select="$dc/doc:element[@name='creator']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:creator><xsl:value-of select="." /></dc:creator>
			</xsl:for-each>
			<!-- dc.contributor.author -->
			<xsl:for-each select="$dc/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:creator><xsl:value-of select="." /></dc:creator>
			</xsl:for-each>
			<!-- dc.contributor.* (!author) -->
			<xsl:for-each select="$dc/doc:element[@name='contributor']/doc:element[@name!='author']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:contributor><xsl:value-of select="." /></dc:contributor>
			</xsl:for-each>
			<!-- dc.contributor -->
			<xsl:for-each select="$dc/doc:element[@name='contributor']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:contributor><xsl:value-of select="." /></dc:contributor>
			</xsl:for-each>
			<!-- dc.subject -->
			<xsl:for-each select="$dc/doc:element[@name='subject']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:subject><xsl:value-of select="." /></dc:subject>
			</xsl:for-each>
			<!-- dc.subject.* -->
			<xsl:for-each select="$dc/doc:element[@name='subject']/doc:element/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:subject><xsl:value-of select="." /></dc:subject>
			</xsl:for-each>
			<!-- dcterms.description -->
			<xsl:for-each select="$dcterms/doc:element[@name='description']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:description><xsl:value-of select="." /></dc:description>
			</xsl:for-each>

			<!-- dc.description -->
			<xsl:for-each select="$dc/doc:element[@name='description']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:description><xsl:value-of select="." /></dc:description>
			</xsl:for-each>

			<!-- dcterms.abstract -->
			<xsl:for-each select="$dcterms/doc:element[@name='abstract']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:description>
					<xsl:value-of select="." />
				</dc:description>
			</xsl:for-each>

			<xsl:for-each select="$dcterms/doc:element[@name='tableOfContents']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:description>
					<xsl:value-of select="." />
				</dc:description>
			</xsl:for-each>


			<!-- dc.description.* (not provenance)-->
			<xsl:for-each select="$dc/doc:element[@name='description']/doc:element[@name!='provenance']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:description><xsl:value-of select="." /></dc:description>
			</xsl:for-each>
			<!-- dc.date -->
			<xsl:for-each select="$dc/doc:element[@name='date']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:date><xsl:value-of select="." /></dc:date>
			</xsl:for-each>
			<!-- dc.date.* -->
			<xsl:for-each select="$dc/doc:element[@name='date']/doc:element/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:date><xsl:value-of select="." /></dc:date>
			</xsl:for-each>

			<!-- dc.type -->
			<xsl:for-each
				select="$dc/doc:element[@name = 'type']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
						<xsl:choose>
						<xsl:when
							test="contains(.,'Actes de congrès')"> 
							<dc:type xml:lang="en" rdf:resource="http://purl.org/coar/resource_type/c_f744">conference proceedings</dc:type>
							<dc:type xml:lang="fr" rdf:resource="http://purl.org/coar/resource_type/c_f744">actes de congrès</dc:type>
						</xsl:when>
						<xsl:when
							test="contains(.,'Article')">
							<dc:type xml:lang="en" rdf:resource="http://purl.org/coar/resource_type/c_6501">journal article</dc:type>
							<dc:type xml:lang="fr" rdf:resource="http://purl.org/coar/resource_type/c_6501">article</dc:type>
						</xsl:when>
						<xsl:when
							test="contains(.,'Revue')">
							<dc:type xml:lang="en" rdf:resource="http://purl.org/coar/resource_type/c_0640">journal</dc:type>
							<dc:type xml:lang="fr" rdf:resource="http://purl.org/coar/resource_type/c_0640">revue</dc:type>
						</xsl:when>
						<xsl:when
							test="contains(.,'Livre')"> <!-- Livre avec majuscule donc pas chapitre-->
							<dc:type xml:lang="en" rdf:resource="http://purl.org/coar/resource_type/c_2f33">book</dc:type>
							<dc:type xml:lang="fr" rdf:resource="http://purl.org/coar/resource_type/c_2f33">livre</dc:type>
						</xsl:when>
						<xsl:when
							test="contains(.,'Chapitre de livre')">
							<dc:type xml:lang="en" rdf:resource="http://purl.org/coar/resource_type/c_3248">book part</dc:type>
							<dc:type xml:lang="fr" rdf:resource="http://purl.org/coar/resource_type/c_3248">chapitre de livre</dc:type>
						</xsl:when>
						<xsl:when
							test="contains(.,'Enregistrement musical')">
							<dc:type xml:lang="en" rdf:resource="http://purl.org/coar/resource_type/c_18cd">musical composition</dc:type>
							<dc:type xml:lang="fr" rdf:resource="http://purl.org/coar/resource_type/c_18cd">composition musicale</dc:type>
						</xsl:when>
						<xsl:when
							test="contains(.,'Partition')">
							<dc:type xml:lang="en" rdf:resource="http://purl.org/coar/resource_type/c_18cw">musical notation</dc:type>
							<dc:type xml:lang="fr" rdf:resource="http://purl.org/coar/resource_type/c_18cw">partition</dc:type>
						</xsl:when>
						<xsl:when
							test="contains(.,'Film ou vidéo')">
							<dc:type xml:lang="en" rdf:resource="http://purl.org/coar/resource_type/c_12ce">video</dc:type>
							<dc:type xml:lang="fr" rdf:resource="http://purl.org/coar/resource_type/c_12ce">vidéo</dc:type>
						</xsl:when>
						<xsl:when
							test="contains(.,'Ensemble de données')">
							<dc:type xml:lang="en" rdf:resource="http://purl.org/coar/resource_type/c_ddb1">dataset</dc:type>
							<dc:type xml:lang="fr" rdf:resource="http://purl.org/coar/resource_type/c_ddb1">jeu de données</dc:type>
						</xsl:when>
						<xsl:when
							test="contains(.,'Logiciel')">
							<dc:type xml:lang="en" rdf:resource="http://purl.org/coar/resource_type/c_5ce6">software</dc:type>
							<dc:type xml:lang="fr" rdf:resource="http://purl.org/coar/resource_type/c_5ce6">logiciel</dc:type>
						</xsl:when>
						<xsl:when
							test="contains(.,'Carte géographique')">
							<dc:type xml:lang="en" rdf:resource="http://purl.org/coar/resource_type/c_12cd">map</dc:type>
							<dc:type xml:lang="fr" rdf:resource="http://purl.org/coar/resource_type/c_12cd">carte géographique</dc:type>
						</xsl:when>
						<xsl:when
							test="contains(.,'Rapport')">
							<dc:type xml:lang="en" rdf:resource="http://purl.org/coar/resource_type/c_93fc">report</dc:type>
							<dc:type xml:lang="fr" rdf:resource="http://purl.org/coar/resource_type/c_93fc">rapport</dc:type>
						</xsl:when>
						<xsl:when
							test="contains(.,'Présentation hors congrès')"> <!-- autre -->
							<dc:type xml:lang="en" rdf:resource="http://purl.org/coar/resource_type/c_1843">other</dc:type>
							<dc:type xml:lang="fr" rdf:resource="http://purl.org/coar/resource_type/c_1843">autre</dc:type>
						</xsl:when>
						<xsl:when
							test="contains(.,'Contribution à un congrès')">
							<dc:type xml:lang="en" rdf:resource="http://purl.org/coar/resource_type/c_c94f">conference object</dc:type>
							<dc:type xml:lang="fr" rdf:resource="http://purl.org/coar/resource_type/c_c94f">contribution à une conférence</dc:type>
						</xsl:when>
						<xsl:when
							test="contains(.,'Thèse ou mémoire')"> <!-- en couplant avec le degree je pourrais faire un type plus exlicite : maitrise ou doctorat.... -->
							<dc:type xml:lang="en" rdf:resource="http://purl.org/coar/resource_type/c_46ec">thesis</dc:type>
							<dc:type xml:lang="fr" rdf:resource="http://purl.org/coar/resource_type/c_46ec">thèse</dc:type>
						</xsl:when>
						<xsl:when
							test="contains(.,'Travail étudiant')"> <!-- generique : text...mais est-ce que c'est toujours du text...? -->
							<dc:type xml:lang="en" rdf:resource="http://purl.org/coar/resource_type/c_18cf">text</dc:type>
							<dc:type xml:lang="fr" rdf:resource="http://purl.org/coar/resource_type/c_18cf">texte</dc:type>
						</xsl:when>
						<xsl:when
							test="contains(.,'Matériel didactique')">
							<dc:type xml:lang="en" rdf:resource="http://purl.org/coar/resource_type/c_e059">learning object</dc:type>
							<dc:type xml:lang="fr" rdf:resource="http://purl.org/coar/resource_type/c_e059">objet d'apprentissage</dc:type>
						</xsl:when>
						<xsl:when
							test="contains(.,'Autre')">
							<dc:type xml:lang="en" rdf:resource="http://purl.org/coar/resource_type/c_1843">other</dc:type>
							<dc:type xml:lang="fr" rdf:resource="http://purl.org/coar/resource_type/c_1843">autre</dc:type>
						</xsl:when>
						<xsl:otherwise>
							<dc:type xml:lang="en" rdf:resource="http://purl.org/coar/resource_type/c_1843">other</dc:type>
							<dc:type xml:lang="fr" rdf:resource="http://purl.org/coar/resource_type/c_1843">autre</dc:type>
						</xsl:otherwise>
					</xsl:choose>
			</xsl:for-each>

			<!-- dc.identifier -->
			<xsl:for-each select="$dc/doc:element[@name='identifier']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:identifier><xsl:value-of select="." /></dc:identifier>
			</xsl:for-each>
			<!-- dc.identifier.* -->
			<xsl:for-each select="$dc/doc:element[@name='identifier']/doc:element/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:identifier><xsl:value-of select="." /></dc:identifier>
			</xsl:for-each>
			
			<!-- MHV fevrier 2017 : cas special integration orcid id pour les auteurs de TME -->
			<xsl:for-each select="$UdeM/doc:element[@name = 'ORCIDAuteurThese']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()] and position() = 1]">
			<xsl:variable name="value" select="."/>
				<dc:identifier><xsl:value-of select="concat('https://orcid.org/', $value)"/></dc:identifier>
			</xsl:for-each>
			
					<!-- dc.language -->
			<xsl:for-each select="$dcterms/doc:element[@name='language']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:language><xsl:value-of select="." /></dc:language>
			</xsl:for-each>
			<!-- dc.language.* -->
			<xsl:for-each select="$dc/doc:element[@name='language']/doc:element/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:language><xsl:value-of select="." /></dc:language>
			</xsl:for-each>
			<!-- dc.relation -->
			<xsl:for-each select="$dc/doc:element[@name='relation']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:relation><xsl:value-of select="." /></dc:relation>
			</xsl:for-each>
			<!-- dc.relation.* -->
			<xsl:for-each select="$dc/doc:element[@name='relation']/doc:element/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:relation><xsl:value-of select="." /></dc:relation>
			</xsl:for-each>
			
			<!-- si je me fie au mapping  : https://www.rioxx.net/mappings/crosswalk_rioxx_2_0_openaire_3_0/ -->
			<!--
			<xsl:for-each select="$UdeM/doc:element[@name='UdeM']/doc:element[@name='VersionRioxx']/doc:element/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]"
			<dc:relation><xsl:value-of select="." /></dc:relation>
			<xsl:variable name="value" select="."/>
				<xsl:if
					test="contains(normalize-space(.), 'Version originale')">
					<dc:description>info:eu-repo/semantics/publishedVersion</dc:description>
				</xsl:if>
			</xsl:for-each>
			-->
			
			<!-- dc.rights -->
			<xsl:for-each select="$dc/doc:element[@name='rights']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:rights><xsl:value-of select="." /></dc:rights>
			</xsl:for-each>
			<!-- dc.rights.* -->
			<xsl:for-each select="$dc/doc:element[@name='rights']/doc:element/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:rights><xsl:value-of select="." /></dc:rights>
			</xsl:for-each>
			<!-- dc.format -->


		<!-- nombre de formats differents contenus ds ORIGINAL -->
		<xsl:variable name="nombreFormatsDifferents">
				<xsl:value-of select="count($bundles/doc:element[@name = 'bundle']/
									doc:field[@name = 'name' and text() = 'ORIGINAL']/../doc:element[@name = 'bitstreams']/
									doc:element[@name = 'bitstream'][not(doc:field[@name = 'format']=preceding-sibling::
										doc:element[@name = 'bitstream']/doc:field[@name = 'format']
										)])"/>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="($nombreFormatsDifferents and $nombreFormatsDifferents >= 1)">
				<dc:format>
								<xsl:for-each
									select="$bundles/doc:element[@name = 'bundle']/
									doc:field[@name = 'name' and text() = 'ORIGINAL']/../doc:element[@name = 'bitstreams']/
									doc:element[@name = 'bitstream'][not(doc:field[@name = 'format']=preceding-sibling::
										doc:element[@name = 'bitstream']/doc:field[@name = 'format']
										)]">
										<xsl:value-of
										select="./doc:field[@name = 'format']"/>
										<xsl:choose>
											<xsl:when test="position() = last() - 1"> et </xsl:when>
											<xsl:when test="position() != last()">, </xsl:when>
										</xsl:choose>
								</xsl:for-each>
				</dc:format>
			</xsl:when>
			<xsl:otherwise /> 
		</xsl:choose>

			<!-- dc.coverage -->
			<xsl:for-each select="$dc/doc:element[@name='coverage']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:coverage><xsl:value-of select="." /></dc:coverage>
			</xsl:for-each>
			<!-- dc.coverage.* -->
			<xsl:for-each select="$dc/doc:element[@name='coverage']/doc:element/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:coverage><xsl:value-of select="." /></dc:coverage>
			</xsl:for-each>
			<!-- dc.publisher -->
			<xsl:for-each select="$dc/doc:element[@name='publisher']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:publisher><xsl:value-of select="." /></dc:publisher>
			</xsl:for-each>
			<!-- dc.publisher.* -->
			<xsl:for-each select="$dc/doc:element[@name='publisher']/doc:element/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:publisher><xsl:value-of select="." /></dc:publisher>
			</xsl:for-each>
			<!-- dc.source -->
			<xsl:for-each select="$dc/doc:element[@name='source']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:source><xsl:value-of select="." /> </dc:source>
			</xsl:for-each>
			<!-- dc.source.* -->
			<xsl:for-each select="$dc/doc:element[@name='source']/doc:element/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:source><xsl:value-of select="." /></dc:source>
			</xsl:for-each>
		</oai_dc:dc>
	</xsl:template>
</xsl:stylesheet>
