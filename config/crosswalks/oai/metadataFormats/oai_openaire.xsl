<?xml version="1.0" encoding="UTF-8" ?>
<!--
	The contents of this file are subject to the license and copyright detailed
	in the LICENSE and NOTICE files at the root of the source tree and available
	online at http://www.dspace.org/license/
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:oaire="http://namespace.openaire.eu/schema/oaire/"
    xmlns:datacite="http://datacite.org/schema/kernel-4"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:doc="http://www.lyncode.com/xoai"
    xmlns:rdf="http://www.w3.org/TR/rdf-concepts/"
    xmlns:exsl="http://exslt.org/common"
    extension-element-prefixes="exsl"
    version="1.0">
    <xsl:output omit-xml-declaration="yes" method="xml" indent="yes"/>

    <!--  Please note that this crosswalk is mostly a backport from the DSpace 7 version
    	  available here https://github.com/DSpace/DSpace/blob/master/dspace/config/crosswalks/oai/metadataFormats/oai_openaire.xsl
          adapted to work with the limitation of the flat data model of DSpace pre version 7 -->

    <!--  Marie-Helene Vezina, Bibliotheques, Université de Montréal
    Octobre 2020 : Réécriture du schéma developpé par 4science pour CARL-ABRC : https://www.carl-abrc.ca/news/release-of-dspace-5-6-extension-openaire/
    
    Pour adaptation aux éléments actuellement utilisés dans Papyrus. Il s'agit d'éléments provenant des schémas : 
    dc, dcterms, ETDMs, OpenAire, et schéma maison ("UdeM)(dont certains éléments alignés sur la norme Rioxx).
    
    Ce crosswalk s'applique à associer nos métadonnées internes à la norme OpenAire v4
    (https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/application_profile.html) -->

	<xsl:variable name="UdeM" select="doc:metadata/doc:element[@name = 'UdeM']"/>
	<xsl:variable name="dc" select="doc:metadata/doc:element[@name = 'dc']"/>
	<xsl:variable name="dcterms" select="doc:metadata/doc:element[@name = 'dcterms']"/>
	<xsl:variable name="etdms"
		select="doc:metadata/doc:element[@name = 'etd']/doc:element[@name = 'degree']"/>
	<xsl:variable name="bundles" select="doc:metadata/doc:element[@name = 'bundles']"/>
	<xsl:variable name="oaire" select="doc:metadata/doc:element[@name = 'oaire']"/>
	<xsl:variable name="others" select="doc:metadata/doc:element[@name = 'others']"/>

	<xsl:variable name="estUneTME">
		<xsl:for-each
			select="$dc/doc:element[@name = 'type']/doc:element/doc:field[@name = 'value']">
			<xsl:if test="contains(normalize-space(.), 'Thèse ou mémoire')">
				<xsl:value-of select="'true'"/>
			</xsl:if>
		</xsl:for-each>
	</xsl:variable>

	<xsl:variable name="level">
		<xsl:for-each select="$etdms/doc:element[@name = 'level']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<xsl:choose>
					<xsl:when test="contains(.,'Maîtrise')">
					<xsl:value-of select="'Maîtrise'"/>
					</xsl:when>
					<xsl:when test="contains(., 'Doctorat')">
					<xsl:value-of select="'Doctorat'"/>
					</xsl:when>
					<xsl:otherwise>
					</xsl:otherwise>
				</xsl:choose>
		</xsl:for-each>
	</xsl:variable>

<!-- testons si il y a un embargo (donc un date.issued et un date.available avec la chaine "MONTHS_WITHHELD" et
si le nb de mois est bien une valeur numérique-->

   <xsl:variable name="dateAvailable">
		<xsl:for-each
			select="$dc/doc:element[@name = 'date']/doc:element[@name = 'available']/doc:element/doc:field[@name = 'value']">
			<xsl:if test="contains(normalize-space(.), 'MONTHS_WITHHELD')">
				<xsl:value-of select="normalize-space(substring-after(., ':'))"/>
			</xsl:if>
		</xsl:for-each>
   </xsl:variable>

        <xsl:variable name="dateIssued">
					<xsl:if test="string-length($dc/doc:element[@name = 'date']/doc:element[@name = 'issued'][position() = 1]/doc:element/doc:field[@name = 'value']/text()) > 0">
						<xsl:value-of
							select="substring($dc/doc:element[@name = 'date']/doc:element[@name = 'issued'][position() = 1]/doc:element/doc:field[@name = 'value']/text(), 1, 10)"/>
					</xsl:if>
        </xsl:variable>



	<xsl:variable name="apostrophe">'</xsl:variable>

	<xsl:param name="uppercase"
		select="'ABCDEFGHIJKLMNOPQRSTUVWXYZÀÈÌÒÙÁÉÍÓÚÝÂÊÎÔÛÃÑÕÄËÏÖÜŸÅÆŒÇÐØ'" />

	<xsl:param name="smallcase"
		select="'abcdefghijklmnopqrstuvwxyzàèìòùáéíóúýâêîôûãñõäëïöüÿåæœçðø'" />




	<xsl:template match="/">
		<oaire:resource
			xmlns:vc="http://www.w3.org/2007/XMLSchema-versioning"
			xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			xsi:schemaLocation="http://namespace.openaire.eu/schema/oaire/ https://www.openaire.eu/schema/repo-lit/4.0/openaire.xsd">

			<!-- 1. TITLE (M)-->
			<xsl:for-each
				select="$dc/doc:element[@name='title']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<datacite:title>
					<xsl:value-of select="." />
				</datacite:title>
			</xsl:for-each>
			<!-- MHV : J'ajoute un traitement pour nos titres associés (parallèles, traduits, abrégés) mais qui ne sont pas des sous-titres -->
			<xsl:for-each
			select="$dcterms/doc:element[@name = 'alternative']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<datacite:title titleType="AlternativeTitle">
					<xsl:value-of select="." />
				</datacite:title>
			</xsl:for-each>

			<!-- 2. CREATORS (M) -->
			<xsl:apply-templates
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']"
				mode="datacite"/>
				
			<!-- Metadonnée obligatoire dans schema OpenAire 4.0.
			Nous avons qqs cas de rapports, bulletins, revues entières pour lesquelles nous n'avons *pas* de dc.contributor.author
			Dans ces cas je vais forger un dc.creator en prenant le dc.publisher, qui devrait exister...-->

		<xsl:variable name="NbpublishersNonVides"
			select="count(doc:metadata/doc:element[@name='dc']/doc:element[@name = 'publisher']/doc:element/doc:field[@name = 'value']/text()[normalize-space()])" />
		<xsl:variable name="NbauthorsNonVides"
			select="count(doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name = 'value']/text()[normalize-space()])" />

			<xsl:choose>
				<xsl:when test="$NbauthorsNonVides = 0 and $NbpublishersNonVides >= 1">
					<datacite:creators>
						<xsl:for-each
							select="doc:metadata/doc:element[@name='dc']  /doc:element[@name='publisher']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
							<datacite:creator>
								<xsl:value-of select="." />
							</datacite:creator>
						</xsl:for-each>
					</datacite:creators>
				</xsl:when>
			</xsl:choose>


			<!-- 3. CONTRIBUTOR (MA) -->

			<datacite:contributors>
			<!-- MHV : Le cas de nos contributeurs divers non directeurs de theses (Éditeur intellectuel, al.) -->

				<xsl:for-each
					select="doc:metadata/doc:element[@name='dc']/doc:element[@name = 'contributor']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
					<datacite:contributor>
						<xsl:attribute name="contributorType">
							<xsl:text>Other</xsl:text>
						</xsl:attribute>
						<datacite:contributorName>
							<xsl:value-of select="./doc:field[@name='value']/text()"/>
						</datacite:contributorName>
					</datacite:contributor>
				</xsl:for-each>

				<!-- MHV 2020 : Le cas de nos directeurs de theses -->
				<xsl:for-each
					select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='advisor']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
					<datacite:contributor>
						<xsl:attribute name="contributorType">
							<xsl:text>Supervisor</xsl:text>
						</xsl:attribute>
						<datacite:contributorName>
						<xsl:attribute name="nameType">
							<xsl:text>Personal</xsl:text>
						</xsl:attribute>
							<xsl:value-of select="./text()" />
						</datacite:contributorName>
					</datacite:contributor>
				</xsl:for-each>

				<!-- MHV 2020 : Le cas de nos "affiliations de document".
				Je ne peux produire "datacite:affiliation" de l'élément creator ne sachant pas exactement quelle affiliation appartient à quel auteur (parmi une liste par ex.) -->

				<xsl:for-each
					select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='affiliation']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
					<datacite:contributor>
						<xsl:attribute name="contributorType">
							<xsl:text>Other</xsl:text>
						</xsl:attribute>
						<datacite:contributorName>
						<xsl:attribute name="nameType">
							<xsl:text>Organizational</xsl:text>
						</xsl:attribute>
							<xsl:value-of select="./text()" />
						</datacite:contributorName>
					</datacite:contributor>
				</xsl:for-each>

			</datacite:contributors>

			<!-- 4. FUNDING REFERENCE  (MA) -->

				<!-- MHV 2020 : Information non saisie dans Papyrus actuellement.-->


			<!-- 5. ALTERNATE IDENTIFIER (R)-->

					<xsl:variable name="handlenatif">
						<xsl:value-of select="doc:metadata/doc:element[@name='others']/doc:field[@name = 'handle' and descendant::text()[normalize-space()]]"/>
					</xsl:variable>

			<datacite:alternateIdentifiers>
				<xsl:for-each
					select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">

					<!-- Dspace met le handle du document dans le dc.identifier; on ne veut pas l'outputer dans les alternate identifier, seul dans datacite:identifier -->
					<xsl:if test=". != concat('http://hdl.handle.net/',$handlenatif)">

					<xsl:variable name="alternateIdentifierType">
						<xsl:call-template name="getRelatedIdentifierType">
							<xsl:with-param name="element" select="../.." />
						</xsl:call-template>
					</xsl:variable>

					<datacite:alternateIdentifier>
						<xsl:attribute name="alternateIdentifierType">
							<xsl:value-of select="$alternateIdentifierType" />
						</xsl:attribute>
						<xsl:value-of
							select="./text()" />
					</datacite:alternateIdentifier>

				</xsl:if>
				</xsl:for-each>
			</datacite:alternateIdentifiers>

			<!-- MHV 2020 :
			
			Puisque nous n'utilisons pas le dc.identifier.issn dans le cas d'article de périodique mais
			*seulement* pour les numero entier de periodique; je mettrai donc le dc.identifier.issn en tant que identifier alternative (alternative au handle du numero de la revue s'entend)
			
			Dans le cas des articles de périodiques on mets le ISSN en tant que related identifier,
			je change ce code pour l'appliquer plutot aux dcterms.isPartOf:
			
			Enfin, je mets aussi nos quelques dcterms.hasVersion (autre versions linguistiques) et dcterms.relation
			(ensembles de données de recherche liés)
			en tant que related identifier
			-->

			 <!-- 6. RELATED IDENTIFIERS (R)-->

			 <!-- MHV le dataset relation qui etait dans OpenAIRE Guidelines for Literature Repositories v3 n'a pas été
			 reconduit en 4.0. On utilisera donc plutôt un related identifier avec un type de relation "hasPart"
			 Note info de Julian Gautier (Dataverse) :
			 https://groups.google.com/g/dataverse-community/c/tWYAijHAgBE/m/OgnTuGzGEAAJ
			 "So this XML could come from a repository that uses HasPart to indicate the file's relationship to the dataset.
			 and the repository is using IsSupplementTo to indicate any relationship a dataset has with a research article (with the resourceTypeGeneral="Text")." -->
			 
			<datacite:relatedIdentifiers>
			<xsl:apply-templates
			 select="doc:metadata/doc:element[@name='dcterms']/doc:element[@name='isPartOf' or @name='hasVersion' or @name='relation']/doc:element"
			 mode="datacite_related_ids"/>
			</datacite:relatedIdentifiers>

			<!-- 7. EMBARGO PERIOD DATES (MA) & 10. PUBLICATION DATE (M)-->
			<!-- MHV. Puisqu'on utilise une fonction maison pour les dates d'embargo et non others.drm il faut reprogrammer le tout.
			
			Notre date de debut d.embargo correspond tjrs, dans notre cas,
			à la date de publication (pour un article) et la date d'octroi de grade (pour une these),
			toutes 2 des informations mises dans dc.date.issued, Ca sera redondant mais pas inexact...
			La norme OpenAire ne retient que 3 types de dates (sous ensemble de DataCite): https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/latest/vocab_datetype.html#vocab-datetype-datetype -->

     <datacite:dates>
			<!-- issue date -->
			<xsl:if test="$dateIssued">
				<datacite:date>
					<xsl:attribute name="dateType">Issued</xsl:attribute>
					<xsl:value-of select="$dateIssued" />
				</datacite:date>
			</xsl:if>

			<xsl:if test="$dateIssued and (string(number($dateAvailable)) != 'NaN')">
                    <datacite:date>
                    	<xsl:attribute name="dateType">Accepted</xsl:attribute>
                        <xsl:value-of select="$dateIssued"/>
                    </datacite:date>
                    <datacite:date>
	                   	<xsl:attribute name="dateType">Available</xsl:attribute>
							            <xsl:call-template name="AjouterMoisAUneDate">
							                <xsl:with-param name="dateTime" select="$dateIssued"/>
							                <xsl:with-param name="months-to-add" select="$dateAvailable"/>
							            </xsl:call-template>
                    </datacite:date>
            </xsl:if>
   </datacite:dates>

			<!-- 8. LANGUAGE (MA)-->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dcterms']/doc:element[@name='language']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:language>
					<xsl:value-of select="." />
				</dc:language>
			</xsl:for-each>


			<!-- 9. PUBLISHER (MA)  -->
<!--MHV. Puisqu'on consièse nos thèses comme des documents "publiés". je vais ajouter le etd.degree.grantor ici.

Rappel:
-	Pour le type « Thèse ou mémoire » : nous considérons le document comme « publié » donc 264 B1 (Mention de publication)
-	Pour le type « Article » :
	UdeM.VersionRioxx «VoR », « EVoR » ou « CVoR » : nous considérons le document comme « publié » donc 264 B1 (Mention de publication)
	UdeM.VersionRioxx «AO», « AM » ou «NA » : nous considérons le document comme « non publié » donc 264 B0 (Mention de production)
-	Pour tout autre type de document :
	élément « dc.publisher » renseigné : nous considérons le document comme « publié » donc 264 B1 (Mention de publication)
	élément « dc.publisher » vide : nous considérons le document comme « non publié » donc 264 B0 (Mention de production)
-->

			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:publisher>
					<xsl:value-of select="." />
				</dc:publisher>
			</xsl:for-each>

			<xsl:for-each
				select="$etdms/doc:element[@name='grantor']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:publisher>
					<xsl:value-of select="." />
				</dc:publisher>
			</xsl:for-each>


			<!-- 11. RESOURCE TYPE (M)-->
				<xsl:apply-templates
					select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']"
					mode="oaire"/>

			<!-- 12 DESCRIPTION (MA) -->
			<!-- Abstract -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dcterms']/doc:element[@name='abstract']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:description>
					<xsl:value-of select="." />
				</dc:description>
			</xsl:for-each>

			<!-- Table of contents -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dcterms']/doc:element[@name='tableOfContents']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:description>
					<xsl:value-of select="." />
				</dc:description>
			</xsl:for-each>

			<!-- Note publique générale.... j'ajoute ici -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dcterms']/doc:element[@name='description']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<dc:description>
					<xsl:value-of select="." />
				</dc:description>
			</xsl:for-each>

			<!-- 13 FORMAT (R) -->
		<xsl:call-template name="format"/>

			<!-- 14. RESOURCE IDENTIFIER (M) -->
			<datacite:identifier>
				<xsl:attribute name="identifierType">
					<xsl:text>Handle</xsl:text>
				</xsl:attribute>
					<xsl:value-of select= "concat('http://hdl.handle.net/',doc:metadata/doc:element[@name='others']/doc:field[@name = 'handle' and descendant::text()[normalize-space()]])"/>
			</datacite:identifier>

			<!-- 15. ACCESS RIGHTS (M) -->
			<!-- MHV 2020 : Nous n'utilisons pas others.drm. Je commente -->
			<xsl:call-template name="accessrights"/>

			<!-- 16. SOURCE (R) -->
			<!-- MHV 2020 : N/A -->


			<!-- 17 SUBJECT (MA)-->
			<xsl:apply-templates
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']"
				mode="datacite">
			</xsl:apply-templates>


			<!-- 18 LICENSE CONDITION (R) -->
			<!-- MHV 2020 : il faut inclure ici dc.rights / dc.rights.uri ; en principe on a un seul dc.rights et un seul dc.rights.uri -->

			<xsl:call-template
				name="licensecondition">
			</xsl:call-template>

			<!-- 19. COVERAGE (R) -->
			<!-- MHV 2020 : N/A -->


			<!-- 20. SIZE (O)-->
			<!-- Note MHV : Dans Dspace la taille du fichier est toujours exprimée en octets et nécessite donc une conversion
			/ In Dspace file size always comes in bytes and thus needs conversion -->

			<xsl:apply-templates
				select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']"
				mode="datacite">
			</xsl:apply-templates>


			<!-- 21. GEO LOCATION (O) -->
			<!-- MHV 2020 : N/A -->

			<!-- 22. RESSOURCE VERSION (R) -->
			<xsl:call-template
				name="resourceversion">
			</xsl:call-template>


			<!-- 23. FILE LOCATION (MA)-->

			<xsl:for-each
				select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']">
				<xsl:if test="doc:field[@name='name']/text() = 'ORIGINAL'">
					<xsl:for-each
						select="doc:element[@name='bitstreams']/doc:element">
						<oaire:file>
							<!--<xsl:attribute name="accessRightsURI">
								<xsl:value-of select="doc:field[@name='drm']/text()"/>
							</xsl:attribute> -->
							<xsl:attribute name="mimeType">
								<xsl:value-of
									select="doc:field[@name='format']/text()"/>
							</xsl:attribute>
							<xsl:value-of select="doc:field[@name='url']"/>
						</oaire:file>
					</xsl:for-each>
				</xsl:if>
			</xsl:for-each>


			<!-- 24.- 31. CITATION* (R)-->
			<!-- MHV 2020 : Adoption interne des elements openaire citation* dans Papyrus. Il n'y a plus aucune traces de dc.ispartofseries, dcterms.bibliographicCitation, dc.identifier.citation ! -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='oaire']/doc:element[@name='citationTitle']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
						<oaire:citationTitle>
							<xsl:value-of select="."/>
						</oaire:citationTitle>
			</xsl:for-each>

			<xsl:for-each
				select="doc:metadata/doc:element[@name='oaire']/doc:element[@name='citationVolume']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
						<oaire:citationVolume>
							<xsl:value-of select="."/>
						</oaire:citationVolume>
			</xsl:for-each>

			<xsl:for-each
				select="doc:metadata/doc:element[@name='oaire']/doc:element[@name='citationIssue']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
						<oaire:citationIssue>
							<xsl:value-of select="."/>
						</oaire:citationIssue>
			</xsl:for-each>

			<xsl:for-each
				select="doc:metadata/doc:element[@name='oaire']/doc:element[@name='citationStartPage']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
						<oaire:citationStartPage>
							<xsl:value-of select="."/>
						</oaire:citationStartPage>
			</xsl:for-each>

			<xsl:for-each
				select="doc:metadata/doc:element[@name='oaire']/doc:element[@name='citationEndPage']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
						<oaire:citationEndPage>
							<xsl:value-of select="."/>
						</oaire:citationEndPage>
			</xsl:for-each>

			<xsl:for-each
				select="doc:metadata/doc:element[@name='oaire']/doc:element[@name='citationEdition']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
						<oaire:citationEdition>
							<xsl:value-of select="."/>
						</oaire:citationEdition>
			</xsl:for-each>
			

			<xsl:for-each
				select="doc:metadata/doc:element[@name='oaire']/doc:element[@name='citationConferencePlace']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
						<oaire:citationConferencePlace>
							<xsl:value-of select="."/>
						</oaire:citationConferencePlace>
			</xsl:for-each>

			<xsl:for-each
				select="doc:metadata/doc:element[@name='oaire']/doc:element[@name='citationConferenceDate']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
						<oaire:citationConferenceDate>
							<xsl:value-of select="."/>
						</oaire:citationConferenceDate>
			</xsl:for-each>


<!--
			<xsl:variable name="rightsURI">
				<xsl:call-template name="resolveRightsURI">
					<xsl:with-param name="field"
							select="doc:metadata/doc:element[@name='others']/doc:field[@name='drm']" />
				</xsl:call-template>
			</xsl:variable>
-->


		</oaire:resource>
	</xsl:template>


	<xsl:template name="accessrights">
		<xsl:choose>
		<xsl:when test="$dateIssued and (string(number($dateAvailable)) != 'NaN')">
			<datacite:rights rightsURI="http://purl.org/coar/access_right/c_f1cf">embargoed access</datacite:rights>
    </xsl:when>
		<xsl:otherwise>
			<datacite:rights rightsURI="http://purl.org/coar/access_right/c_abf2">open access</datacite:rights>
		</xsl:otherwise>
		</xsl:choose>
	</xsl:template>




	<xsl:template
		match="doc:element[@name='bundles']/doc:element[@name='bundle']"
		mode="datacite">
		<xsl:if test="doc:field[@name='name' and text()='ORIGINAL']">
			<datacite:sizes>
			<datacite:size>
				<xsl:for-each
					select="doc:element[@name='bitstreams']/doc:element[@name='bitstream']">
									<xsl:variable name="taille" select="doc:field[@name='size']"/>
                        <xsl:choose>
                            <xsl:when test="$taille &lt; 1024">
                                <xsl:value-of select="$taille"/>
                                <xsl:text> bytes</xsl:text><!-- bytes-->
                            </xsl:when>
                            <xsl:when test="$taille &lt; 1024 * 1024">
                              <xsl:value-of select="format-number((number($taille) div 1024),'#.000')"/>
                                <xsl:text> KB</xsl:text><!-- KB-->
	                            </xsl:when>
                            <xsl:when test="$taille &lt; 1024 * 1024 * 1024">
                                <xsl:value-of
                                    select="format-number((number($taille) div (1024 * 1024)),'#.000')"/>
                                <xsl:text> MB</xsl:text><!-- MB-->
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                    select="format-number((number($taille) div (1024 * 1024 * 1024)),'#.000')"/>
                                <xsl:text> GB</xsl:text><!-- GB-->
                            </xsl:otherwise>
                        </xsl:choose>
										<xsl:choose>
											<xsl:when test="position() = last() - 1"> and </xsl:when><!-- and -->
											<xsl:when test="position() != last()">, </xsl:when>
										</xsl:choose>
				</xsl:for-each>
			</datacite:size>
			</datacite:sizes>
		</xsl:if>
	</xsl:template>

		<!-- datacite:subjects -->
	<!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_subject.html -->
			<xsl:template
		match="doc:element[@name='dc']/doc:element[@name='subject']"
		mode="datacite">
		<datacite:subjects>
			<xsl:for-each select="./doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<datacite:subject>
					<xsl:value-of select="." />
				</datacite:subject>
			</xsl:for-each>
		</datacite:subjects>
	</xsl:template>

<!--
	<xsl:template
		match="doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='issn']"
		mode="datacite_ids">
			<datacite:relatedIdentifiers>
				<datacite:relatedIdentifier>
					<xsl:attribute name="relatedIdentifierType">ISSN</xsl:attribute>
					<xsl:attribute name="relationType">IsPartOf</xsl:attribute>
					<xsl:value-of
						select="./doc:element/doc:field[@name='value']" />
				</datacite:relatedIdentifier>
			</datacite:relatedIdentifiers>
	</xsl:template>
-->



	<xsl:template
		match="doc:element[@name='dcterms']/doc:element[@name='isPartOf' or @name='hasVersion' or @name='relation']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]"
		mode="datacite_related_ids">
		<xsl:variable name="relationType">
								<xsl:choose>
										<xsl:when test="contains((../../@name), 'isPartOf')"><xsl:text>IsPartOf</xsl:text></xsl:when>
											<xsl:when test="contains((../../@name), 'hasVersion')"><xsl:text>HasVersion</xsl:text></xsl:when>
											<xsl:when test="contains((../../@name), 'relation')"><xsl:text>HasPart</xsl:text></xsl:when>
								</xsl:choose>
		</xsl:variable>
		<xsl:variable name="resourceTypeGeneral"><!-- nous n'avons que des ensemble de données pour cet élément -->
								<xsl:choose>
											<xsl:when test="contains((../../@name), 'relation')"><xsl:text>Dataset</xsl:text></xsl:when>
								</xsl:choose>
		</xsl:variable>
								<xsl:choose>
										<xsl:when test="starts-with(., 'urn:ISSN')">
												<datacite:relatedIdentifier>
																<xsl:attribute name="relationType"><xsl:value-of select="$relationType"/></xsl:attribute>
																<xsl:attribute name="relatedIdentifierType">ISSN</xsl:attribute>
																<xsl:value-of
																	select="substring-after(.,'urn:ISSN:')" />
												</datacite:relatedIdentifier>
										</xsl:when>
										<xsl:when test="starts-with(., 'urn:ISBN')">
												<datacite:relatedIdentifier>
																<xsl:attribute name="relationType"><xsl:value-of select="$relationType"/></xsl:attribute>
																<xsl:attribute name="relatedIdentifierType">ISBN</xsl:attribute>
																<xsl:value-of
																	select="substring-after(.,'urn:ISBN:')" />
												</datacite:relatedIdentifier>
										</xsl:when>
										<xsl:when test="starts-with(., 'doi:')">
												<datacite:relatedIdentifier>
																<xsl:attribute name="relationType"><xsl:value-of select="$relationType"/></xsl:attribute>
																<xsl:if test="$resourceTypeGeneral != ''">
																	<xsl:attribute name="resourceTypeGeneral"><xsl:value-of select="$resourceTypeGeneral"/></xsl:attribute>
																</xsl:if>
																<xsl:attribute name="relatedIdentifierType">DOI</xsl:attribute>
																<xsl:value-of
																	select="substring-after(.,'doi:')" />
												</datacite:relatedIdentifier>
										</xsl:when>
										<xsl:when test="contains(.,'hdl.handle.net')">
												<datacite:relatedIdentifier>
																<xsl:attribute name="relationType"><xsl:value-of select="$relationType"/></xsl:attribute>
																<xsl:if test="$resourceTypeGeneral != ''">
																	<xsl:attribute name="resourceTypeGeneral"><xsl:value-of select="$resourceTypeGeneral"/></xsl:attribute>
																</xsl:if>
																<xsl:attribute name="relatedIdentifierType">Handle</xsl:attribute>
																<xsl:value-of
																	select="substring-after(.,'hdl.handle.net/')" />
												</datacite:relatedIdentifier>
										</xsl:when>
										<xsl:when test="starts-with(.,'http')">
												<datacite:relatedIdentifier>
																<xsl:attribute name="relationType"><xsl:value-of select="$relationType"/></xsl:attribute>
																<xsl:if test="$resourceTypeGeneral != ''">
																	<xsl:attribute name="resourceTypeGeneral"><xsl:value-of select="$resourceTypeGeneral"/></xsl:attribute>
																</xsl:if>
																<xsl:attribute name="relatedIdentifierType">URL</xsl:attribute>
																<xsl:value-of
																	select="." />
												</datacite:relatedIdentifier>
										</xsl:when>
								</xsl:choose>
	</xsl:template>


	<xsl:template
		match="doc:element[@name='dc']/doc:element[@name='identifier']/doc:element/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]"
		mode="datacite_ids">

		<xsl:variable name="alternateIdentifierType">
			<xsl:call-template name="getRelatedIdentifierType">
				<xsl:with-param name="element" select="../.." />
			</xsl:call-template>
		</xsl:variable>
			<datacite:alternateIdentifier>
				<xsl:attribute name="alternateIdentifierType">
					<xsl:value-of select="$alternateIdentifierType" />
				</xsl:attribute>
				<xsl:value-of
					select="./text()" />
			</datacite:alternateIdentifier>

	</xsl:template>




	<xsl:template name="licensecondition">
	<!--MHV. OpenAire ne permet qu'un seul élément oaire:licenseCondition mais il pourrait y avoir dans notre Dspace
	plusieurs éléments dc.rights et dc.rights.uri; je vais donc concaténer tous les dc.rights et tous les dc.rights.uri avec un separateur.... pas beau certes
	en pratique ca ne devrait pas arriver. Aussi nous avons des notices avec uniquement un dc.rights.uri. Je fais en sorte de les afficher en attribut, avec l'élément vide.-->

	
		<xsl:variable name="rightsURI">
		<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element[@name='uri']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
		            <xsl:value-of select="."/>
								<xsl:if test="position() != last()"> ; </xsl:if>
		</xsl:for-each>
		</xsl:variable>

		<xsl:variable name="rights">
		<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
		            <xsl:value-of select="."/>
									<xsl:if test="position() != last()">// </xsl:if>
		</xsl:for-each>
		</xsl:variable>


		<xsl:if test="$rightsURI != '' or $rights != ''">

		<oaire:licenseCondition>
			<xsl:attribute name="startDate">
				<xsl:value-of
					select="$dateIssued"/>
			</xsl:attribute>

		<xsl:if test="$rightsURI != ''">
			<xsl:attribute name="uri">
				<xsl:value-of
					select="$rightsURI" />
		</xsl:attribute>
		</xsl:if>

		<xsl:if test="$rights != ''">
				<xsl:value-of
					select="$rights" />
		</xsl:if>

<!--
		<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
		            <xsl:value-of select="."/>
									<xsl:if test="position() != last()">// </xsl:if>
		</xsl:for-each> -->
		</oaire:licenseCondition>

		</xsl:if>
	</xsl:template>


	<xsl:template name="resourceversion">
			<xsl:for-each select="doc:metadata/doc:element[@name='UdeM']/doc:element[@name='VersionRioxx']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">

			<xsl:variable name="valeuren">
				<xsl:value-of select="normalize-space(substring-after((.), '/'))"/>
			</xsl:variable>

			<oaire:version>
		<xsl:choose>
			<xsl:when test="$valeuren != '' and starts-with($valeuren,    concat('Author', $apostrophe,'s Original')       )">
				<xsl:attribute name="uri"><xsl:value-of select="'http://purl.org/coar/version/c_b1a7d7d4d402bcce'" /></xsl:attribute>
				<xsl:text>AO</xsl:text>
			</xsl:when>
			<xsl:when test="$valeuren != '' and starts-with($valeuren,'Submitted Manuscript Under Review')">
				<xsl:attribute name="uri"><xsl:value-of select="'http://purl.org/coar/version/c_71e4c1898caa6e32'" /></xsl:attribute>
				<xsl:text>SMUR</xsl:text>
			</xsl:when>
			<xsl:when test="$valeuren != '' and starts-with($valeuren,'Accepted Manuscript')">
				<xsl:attribute name="uri"><xsl:value-of select="'http://purl.org/coar/version/c_ab4af688f83e57aa'" /></xsl:attribute>
				<xsl:text>AM</xsl:text>
			</xsl:when>
			<xsl:when test="$valeuren != '' and starts-with($valeuren,'Proof')">
				<xsl:attribute name="uri"><xsl:value-of select="'http://purl.org/coar/version/c_fa2ee174bc00049f'" /></xsl:attribute>
				<xsl:text>P</xsl:text>
			</xsl:when>
			<xsl:when test="$valeuren != '' and starts-with($valeuren,'Version of Record')">
				<xsl:attribute name="uri"><xsl:value-of select="'http://purl.org/coar/version/c_970fb48d4fbd8a85'" /></xsl:attribute>
				<xsl:text>VoR</xsl:text>
			</xsl:when>
			<xsl:when test="$valeuren != '' and starts-with($valeuren,'Corrected Version of Record')">
				<xsl:attribute name="uri"><xsl:value-of select="'http://purl.org/coar/version/c_e19f295774971610'" /></xsl:attribute>
				<xsl:text>CVoR</xsl:text>
			</xsl:when>
			<xsl:when test="$valeuren != '' and starts-with($valeuren,'Enhanced Version of Record')">
				<xsl:attribute name="uri"><xsl:value-of select="'http://purl.org/coar/version/c_dc82b40f9837b551'" /></xsl:attribute>
				<xsl:text>EVoR</xsl:text>
			</xsl:when>
			<xsl:when test="$valeuren != '' and contains($valeuren,'Unknown')">
				<xsl:attribute name="uri"><xsl:value-of select="'http://purl.org/coar/version/c_be7fb7dd8ff6fe43'" /></xsl:attribute>
				<xsl:text>NA</xsl:text>
			</xsl:when>
					<xsl:otherwise>
				<xsl:attribute name="uri"><xsl:value-of select="'http://purl.org/coar/version/c_be7fb7dd8ff6fe43'" /></xsl:attribute>
				<xsl:text>NA</xsl:text>
					</xsl:otherwise>
		</xsl:choose>
		</oaire:version>
		</xsl:for-each>
	</xsl:template>



	<!-- it will verify if a given field is an handle -->
	<xsl:template name="isHandle">
		<xsl:param name="field" />
		<xsl:choose>
			<xsl:when test="$field[contains(text(),'hdl.handle.net')]">
				<xsl:value-of select="true()" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="false()" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- it will verify if a given field is a DOI -->
	<xsl:template name="isDOI">
		<xsl:param name="field" />
		<xsl:choose>
			<xsl:when
				test="$field[contains(text(),'doi.org') or starts-with(text(),'10.')]">
				<xsl:value-of select="true()" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="false()" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- it will verify if a given field is an ORCID -->
	<xsl:template name="isORCID">
		<xsl:param name="field" />
		<xsl:choose>
			<xsl:when test="$field[contains(text(),'orcid.org')]">
				<xsl:value-of select="true()" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="false()" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- it will verify if a given field is an URL -->
	<xsl:template name="isURL">
		<xsl:param name="field" />
		<xsl:variable name="lc_field">
			<xsl:call-template name="lowercase">
				<xsl:with-param name="value" select="$field" />
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when
				test="exsl:node-set($lc_field)[starts-with(text(),'http://') or starts-with(text(),'https://')]">
				<xsl:value-of select="true()" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="false()" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- to retrieve a string in lowercase -->
	<xsl:template name="lowercase">
		<xsl:param name="value" />
		<xsl:value-of
			select="translate($value, $uppercase, $smallcase)" />
	</xsl:template>

	<!-- This template will retrieve the identifier type based on the element
		name -->
	<!-- there are some special cases like DOI or HANDLE which the type is also
		inferred from the value itself -->
	<xsl:template name="getRelatedIdentifierType">
		<xsl:param name="element" />
		<xsl:variable name="lc_identifier_type">
			<xsl:call-template name="lowercase">
				<xsl:with-param name="value" select="$element/@name" />
			</xsl:call-template>
		</xsl:variable>


		<xsl:variable name="isHandle">
			<xsl:call-template name="isHandle">
			<!--	<xsl:with-param name="field"
					select="$element/doc:element/doc:field" /> -->
				<xsl:with-param name="field"
					select="." />
			</xsl:call-template>
		</xsl:variable>


		<xsl:variable name="isDOI">
			<xsl:call-template name="isDOI">
				<xsl:with-param name="field"
					select="." />
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="isURL">
			<xsl:call-template name="isURL">
				<xsl:with-param name="field"
					select="." />
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="$lc_identifier_type = 'ark'">
				<xsl:text>ARK</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_identifier_type = 'arxiv'">
				<xsl:text>arXiv</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_identifier_type = 'bibcode'">
				<xsl:text>bibcode</xsl:text>
			</xsl:when>
			<xsl:when
				test="$isDOI = 'true' or $lc_identifier_type = 'doi'">
				<xsl:text>DOI</xsl:text>
			</xsl:when>

			<xsl:when
				test="$isHandle = 'true'">
				<xsl:text>Handle</xsl:text>
			</xsl:when>

			<xsl:when test="$lc_identifier_type = 'ean13'">
				<xsl:text>EAN13</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_identifier_type = 'eissn'">
				<xsl:text>EISSN</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_identifier_type = 'igsn'">
				<xsl:text>IGSN</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_identifier_type = 'isbn'">
				<xsl:text>ISBN</xsl:text>
			</xsl:when>

			<!-- Ajout MHV -->
			<xsl:when test="$lc_identifier_type = 'issn'">
				<xsl:text>ISSN</xsl:text>
			</xsl:when>


			<xsl:when test="$lc_identifier_type = 'istc'">
				<xsl:text>ISTC</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_identifier_type = 'lissn'">
				<xsl:text>LISSN</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_identifier_type = 'lsid'">
				<xsl:text>LSID</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_identifier_type = 'pmid'">
				<xsl:text>PMID</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_identifier_type = 'purl'">
				<xsl:text>PURL</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_identifier_type = 'upc'">
				<xsl:text>UPC</xsl:text>
			</xsl:when>
			<xsl:when
				test="$isURL = 'true' or $lc_identifier_type = 'url' or $lc_identifier_type = 'uri'">
				<xsl:text>URL</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>URN</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>


	 <xsl:template match="doc:element[@name='dc']/doc:element[@name='type']/doc:element"
	 		mode="oaire">

            <xsl:call-template name="resolveResourceTypeTOUT">
                <xsl:with-param name="field" select="./doc:field[@name='value']/text()"/>
            </xsl:call-template>

<!--
        <oaire:resourceType>
            <xsl:attribute name="resourceTypeGeneral">
                <xsl:value-of select="$resourceTypeGeneral"/>
            </xsl:attribute>
            <xsl:attribute name="uri">
                <xsl:value-of select="$resourceTypeURI"/>
            </xsl:attribute>

					<xsl:choose>
            	<xsl:when test="contains(./doc:field[@name='value']/text(),'Thèse ou mémoire')">
										<xsl:choose>
										<xsl:when test="$estUneTME = 'true' and $level = 'Maîtrise'">
										<xsl:text>mémoire de maîtrise</xsl:text>
										</xsl:when>
										<xsl:when test="$estUneTME = 'true' and $level = 'Doctorat'">
										<xsl:text>thèse de doctorat</xsl:text>
										</xsl:when>
										<xsl:otherwise>
										<xsl:text>thèse</xsl:text>
										</xsl:otherwise>
										</xsl:choose>
						</xsl:when>
					<xsl:otherwise>
            <xsl:value-of select="./doc:field[@name='value']/text()"/>
					</xsl:otherwise>
					</xsl:choose>

        </oaire:resourceType>

-->
    </xsl:template>

	    <!--
        This template will return the general type of the resource
        based on a valued text like 'article'
        https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_publicationtype.html#attribute-resourcetypegeneral-m
     -->

    <xsl:template name="TEMPresolveResourceTypeGeneral">
        <xsl:param name="field"/>
        <xsl:variable name="lc_dc_type">
            <xsl:call-template name="lowercase">
                <xsl:with-param name="value" select="$field"/>
            </xsl:call-template>
        </xsl:variable>
<!--
        <xsl:choose>
            <xsl:when test="$lc_dc_type = 'article'">
                <xsl:text>literature</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'journal article'">
                <xsl:text>literature</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'book'">
                <xsl:text>literature</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'book part'">
                <xsl:text>literature</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'book review'">
                <xsl:text>literature</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'dataset'">
                <xsl:text>dataset</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'software'">
                <xsl:text>software</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>other research product</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
-->
        <xsl:choose>
            <xsl:when test="contains($lc_dc_type,'article')">
                <xsl:text>literature</xsl:text>
            </xsl:when>
            <xsl:when test="contains($lc_dc_type,'livre')"> <!-- a la fois livre et chapitre de livre -->
                <xsl:text>literature</xsl:text>
            </xsl:when>
            <xsl:when test="contains($lc_dc_type,'ensemble de données')">
                <xsl:text>dataset</xsl:text>
            </xsl:when>
            <xsl:when test="contains($lc_dc_type,'logiciel')">
                <xsl:text>software</xsl:text>
            </xsl:when>      
            <xsl:otherwise>
                <xsl:text>other research product</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--
        This template will return the COAR Resource Type Vocabulary URI
        like http://purl.org/coar/resource_type/c_6501
        based on a valued text like 'article'
        https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_publicationtype.html#attribute-uri-m
     -->
     <!-- MHV 2020 : ici j'ajuste avec notre liste finie de types, lesquels sont alignés sur le vocabulaire COAR
     La norme recommande l'utilisation, pour la propriété resourceType du label:
     "Use the label of the resource type term as value. In the below table the preferred english
     labels are listed, but labels (preferred or alternative) in other languages can be chosen from the
     COAR Resource Type Vocabulary"
     L'utilisation d'un attribut xml:lang n'est pas possible pour cet élément
     Dans les faits le validateur de OpenAire est capricieux et semble n'accepter que la valeur preferred en anglais (sic).
     Je vais toutefois vérifier si on a these de doctorat ou memoire de maitrise puisque le vocabulaire COAR fait la différence-->

     <!-- MHV nov 2022 : En fonction de la réponse obtenue ici https://github.com/openaire/guidelines-literature-repositories/issues/43
      je change qqs attributs de "other research product" pour "literature" -->

    <xsl:template name="resolveResourceTypeTOUT">
        <xsl:param name="field"/>
        <xsl:variable name="lc_dc_type">
            <xsl:call-template name="lowercase">
                <xsl:with-param name="value" select="$field"/>
            </xsl:call-template>
        </xsl:variable>


        <oaire:resourceType>


        <xsl:choose>
           <!--
            <xsl:when test="$lc_dc_type = 'annotation'">
                <xsl:text>http://purl.org/coar/resource_type/c_1162</xsl:text>
            </xsl:when> 
          -->
            <!--  <xsl:when test="$lc_dc_type = 'journal'"> -->
            <!-- le type COAR "Revue" existe : http://purl.org/coar/resource_type/c_0640 MAIS n'est pas listé dans la liste des valeurs
            admises OpenAire (https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_publicationtype.html);
            https://github.com/openaire/guidelines-literature-repositories/issues/24#issuecomment-726153152 
            -->
            <xsl:when test="contains($lc_dc_type,'revue')">
            <xsl:attribute name="uri">
                <xsl:text>http://purl.org/coar/resource_type/c_0640</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="resourceTypeGeneral">
                <xsl:text>literature</xsl:text>
            </xsl:attribute>
            <xsl:text>journal</xsl:text>
            </xsl:when>

            <!--  <xsl:when test="$lc_dc_type = 'article'"> -->
            <xsl:when test="contains($lc_dc_type,'article')">
            <xsl:attribute name="uri">
                <xsl:text>http://purl.org/coar/resource_type/c_6501</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="resourceTypeGeneral">
                <xsl:text>literature</xsl:text>
            </xsl:attribute>
            <xsl:text>journal article</xsl:text>
            </xsl:when>

           <!--
            <xsl:when test="$lc_dc_type = 'editorial'">
                <xsl:text>http://purl.org/coar/resource_type/c_b239</xsl:text>
            </xsl:when>
          -->
           <!--
            <xsl:when test="$lc_dc_type = 'bachelor thesis'">
                <xsl:text>http://purl.org/coar/resource_type/c_7a1f</xsl:text>
            </xsl:when>
          -->
           <!--
            <xsl:when test="$lc_dc_type = 'bibliography'">
                <xsl:text>http://purl.org/coar/resource_type/c_86bc</xsl:text>
            </xsl:when>
          -->
            <!--  <xsl:when test="$lc_dc_type = 'book'"> -->

            <xsl:when test="contains($lc_dc_type,'livre') and not(contains($lc_dc_type, 'chapitre'))">
            <xsl:attribute name="uri">
                <xsl:text>http://purl.org/coar/resource_type/c_2f33</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="resourceTypeGeneral">
                <xsl:text>literature</xsl:text>
            </xsl:attribute>
            <xsl:text>book</xsl:text>
            </xsl:when>

            <!--  <xsl:when test="$lc_dc_type = 'book part'"> -->
            <xsl:when test="contains($lc_dc_type,'chapitre de livre')">
            <xsl:attribute name="uri">
                <xsl:text>http://purl.org/coar/resource_type/c_3248</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="resourceTypeGeneral">
                <xsl:text>literature</xsl:text>
            </xsl:attribute>
            <xsl:text>book part</xsl:text>
            </xsl:when>

           <!--
            <xsl:when test="$lc_dc_type = 'book review'">
                <xsl:text>http://purl.org/coar/resource_type/c_ba08</xsl:text>
            </xsl:when>
          -->
           <!--
            <xsl:when test="$lc_dc_type = 'website'">
                <xsl:text>http://purl.org/coar/resource_type/c_7ad9</xsl:text>
            </xsl:when>
          -->
           <!--
            <xsl:when test="$lc_dc_type = 'interactive resource'">
                <xsl:text>http://purl.org/coar/resource_type/c_e9a0</xsl:text>
            </xsl:when>
          -->
            <!--  <xsl:when test="$lc_dc_type = 'conference proceedings'"> -->
            <xsl:when test="contains($lc_dc_type,'actes de congrès')">
            <xsl:attribute name="uri">
                <xsl:text>http://purl.org/coar/resource_type/c_f744</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="resourceTypeGeneral">
                <xsl:text>literature</xsl:text>
            </xsl:attribute>
            <xsl:text>conference proceedings</xsl:text>
            </xsl:when>
             <!--  <xsl:when test="$lc_dc_type = 'conference object'"> -->
            <xsl:when test="contains($lc_dc_type,'contribution à un congrès')">
            <xsl:attribute name="uri">
                <xsl:text>http://purl.org/coar/resource_type/c_c94f</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="resourceTypeGeneral">
                <xsl:text>other research product</xsl:text>
            </xsl:attribute>
            <xsl:text>conference object</xsl:text>
            </xsl:when>
           <!--
            <xsl:when test="$lc_dc_type = 'conference paper'">
                <xsl:text>http://purl.org/coar/resource_type/c_5794</xsl:text>
            </xsl:when>
          -->
           <!--
            <xsl:when test="$lc_dc_type = 'conference poster'">
                <xsl:text>http://purl.org/coar/resource_type/c_6670</xsl:text>
            </xsl:when>
          -->

            <!-- <xsl:when test="$lc_dc_type = 'dataset'"> -->
            <xsl:when test="contains($lc_dc_type,'ensemble de données')">
            <xsl:attribute name="uri">
                <xsl:text>http://purl.org/coar/resource_type/c_ddb1</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="resourceTypeGeneral">
                <xsl:text>dataset</xsl:text>
            </xsl:attribute>
            <xsl:text>dataset</xsl:text>
            </xsl:when>


           <!--
            <xsl:when test="$lc_dc_type = 'moving image'">
                <xsl:text>http://purl.org/coar/resource_type/c_8a7e</xsl:text>
            </xsl:when>
          -->
           <!--
            <xsl:when test="$lc_dc_type = 'periodical'">
                <xsl:text>http://purl.org/coar/resource_type/c_2659</xsl:text>
            </xsl:when>
          -->
           <!--
            <xsl:when test="$lc_dc_type = 'letter to the editor'">
                <xsl:text>http://purl.org/coar/resource_type/c_545b</xsl:text>
            </xsl:when>
          -->
           <!--
            <xsl:when test="$lc_dc_type = 'patent'">
                <xsl:text>http://purl.org/coar/resource_type/c_15cd</xsl:text>
            </xsl:when>
          -->
           <!--
            <xsl:when test="$lc_dc_type = 'preprint'">
                <xsl:text>http://purl.org/coar/resource_type/c_816b</xsl:text>
            </xsl:when>
          -->
         <!--   <xsl:when test="$lc_dc_type = 'report'"> -->
            <xsl:when test="contains($lc_dc_type,'rapport')">
            <xsl:attribute name="uri">
                <xsl:text>http://purl.org/coar/resource_type/c_93fc</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="resourceTypeGeneral">
                <xsl:text>literature</xsl:text>
            </xsl:attribute>
            <xsl:text>report</xsl:text>
            </xsl:when>

          <!--  <xsl:when test="$lc_dc_type = 'software'"> -->
            <xsl:when test="contains($lc_dc_type,'logiciel')">
            <xsl:attribute name="uri">
                <xsl:text>http://purl.org/coar/resource_type/c_5ce6</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="resourceTypeGeneral">
                <xsl:text>software</xsl:text>
            </xsl:attribute>
            <xsl:text>software</xsl:text>
            </xsl:when>

<!-- en couplant avec le degree je fait un type plus exlicite : maitrise ou doctorat.... -->
         <!--   <xsl:when test="$lc_dc_type = 'thesis'"> -->
						<xsl:when test="contains($lc_dc_type,'thèse')">
										<xsl:choose>
										<xsl:when test="$estUneTME = 'true' and $level = 'Maîtrise'">
								            <xsl:attribute name="uri">
								                <xsl:text>http://purl.org/coar/resource_type/c_bdcc</xsl:text>
								            </xsl:attribute>
								            <xsl:attribute name="resourceTypeGeneral">
								                <xsl:text>literature</xsl:text>
								            </xsl:attribute>
								            <xsl:text>master thesis</xsl:text>
										</xsl:when>
										<xsl:when test="$estUneTME = 'true' and $level = 'Doctorat'">
								            <xsl:attribute name="uri">
								                <xsl:text>http://purl.org/coar/resource_type/c_db06</xsl:text>
								            </xsl:attribute>
								            <xsl:attribute name="resourceTypeGeneral">
								                <xsl:text>literature</xsl:text>
								            </xsl:attribute>
								            <xsl:text>doctoral thesis</xsl:text>
										</xsl:when>
										<xsl:otherwise>
								            <xsl:attribute name="uri">
								                <xsl:text>http://purl.org/coar/resource_type/c_46ec</xsl:text>
								            </xsl:attribute>
								            <xsl:attribute name="resourceTypeGeneral">
								                <xsl:text>literature</xsl:text>
								            </xsl:attribute>
								            <xsl:text>thesis</xsl:text>
										</xsl:otherwise>
										</xsl:choose>
						</xsl:when>
           <!--
            <xsl:when test="$lc_dc_type = 'cartographic material'">
                <xsl:text>http://purl.org/coar/resource_type/c_12cc</xsl:text>
            </xsl:when>
          -->
           <!-- <xsl:when test="$lc_dc_type = 'map'"> -->
            <xsl:when test="contains($lc_dc_type,'carte géographique')">
            <xsl:attribute name="uri">
                <xsl:text>http://purl.org/coar/resource_type/c_12cd</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="resourceTypeGeneral">
                <xsl:text>other research product</xsl:text>
            </xsl:attribute>
            <xsl:text>map</xsl:text>
            </xsl:when>

           <!-- <xsl:when test="$lc_dc_type = 'video'"> -->
            <xsl:when test="contains($lc_dc_type,'film ou vidéo')">
            <xsl:attribute name="uri">
                <xsl:text>http://purl.org/coar/resource_type/c_12ce</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="resourceTypeGeneral">
                <xsl:text>other research product</xsl:text>
            </xsl:attribute>
            <xsl:text>video</xsl:text>
            </xsl:when>

           <!--  <xsl:when test="$lc_dc_type = 'musical composition'"> -->
            <xsl:when test="contains($lc_dc_type,'enregistrement musical')">
            <xsl:attribute name="uri">
                <xsl:text>http://purl.org/coar/resource_type/c_18cd</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="resourceTypeGeneral">
                <xsl:text>other research product</xsl:text>
            </xsl:attribute>
            <xsl:text>musical composition</xsl:text>
            </xsl:when>
            <!-- MHV Sept 2020 : travail étudiant n'a pas de correpondance; je mets le generique : text... tel que recommandé par le Catalogage (document "Systematisation....)
            mais est-ce que c'est toujours et seulement du texte...je crois que "autre" serait plus avisé.... -->
            <!-- <xsl:when test="$lc_dc_type = 'text'"> -->
            <xsl:when test="contains($lc_dc_type,'travail étudiant')">
            <xsl:attribute name="uri">
                <xsl:text>http://purl.org/coar/resource_type/c_18cf</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="resourceTypeGeneral">
                <xsl:text>other research product</xsl:text>
            </xsl:attribute>
            <xsl:text>text</xsl:text>
            </xsl:when>
           <!--
            <xsl:when test="$lc_dc_type = 'conference paper not in proceedings'">
                <xsl:text>http://purl.org/coar/resource_type/c_18cp</xsl:text>
            </xsl:when>
          -->
           <!--
            <xsl:when test="$lc_dc_type = 'conference poster not in proceedings'">
                <xsl:text>http://purl.org/coar/resource_type/c_18co</xsl:text>
            </xsl:when>
          -->
          <!--  <xsl:when test="$lc_dc_type = 'musical notation'"> -->
            <xsl:when test="contains($lc_dc_type,'partition')">
              <xsl:attribute name="uri">
                <xsl:text>http://purl.org/coar/resource_type/c_18cw</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="resourceTypeGeneral">
                <xsl:text>other research product</xsl:text>
            </xsl:attribute>
            <xsl:text>musical notation</xsl:text>
            </xsl:when>

            <xsl:when test="contains($lc_dc_type,'matériel didactique')">
              <xsl:attribute name="uri">
                <xsl:text>http://purl.org/coar/resource_type/c_e059</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="resourceTypeGeneral">
                <xsl:text>other research product</xsl:text>
            </xsl:attribute>
            <xsl:text>learning object</xsl:text>
            </xsl:when>

            <!-- other -->
            <!-- Présentation hors congrès / Non-conference presentation -->
            <xsl:otherwise>
            <xsl:attribute name="uri">
                <xsl:text>http://purl.org/coar/resource_type/c_1843</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="resourceTypeGeneral">
                <xsl:text>other research product</xsl:text>
            </xsl:attribute>
            <xsl:text>other</xsl:text>
            </xsl:otherwise>
        </xsl:choose>


        </oaire:resourceType>

    </xsl:template>



    <!-- datacite.creators -->
    <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_creator.html -->
    <xsl:template
        match="doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']" mode="datacite">
        <datacite:creators>
            <!-- datacite.creator -->
            <xsl:for-each select="./doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
                <xsl:variable name="isAuthorityValue">
                    <xsl:call-template name="isAuthorityValue">
                        <xsl:with-param name="element" select="."/>
                    </xsl:call-template>
                </xsl:variable>
                <xsl:choose>
                    <!-- if next sibling is authority and starts with virtual:: -->
                    <xsl:when test="$isAuthorityValue = 'true'">
                        <xsl:variable name="entity">
                            <xsl:call-template name="buildAuthorityNode">
                                <xsl:with-param name="element" select="."/>
                            </xsl:call-template>
                        </xsl:variable>
                        <xsl:apply-templates select="$entity" mode="authority_creator"/>
                    </xsl:when>
                    <!-- simple text metadata -->
                    <xsl:otherwise>
                        <datacite:creator>
                            <datacite:creatorName>
                                <xsl:value-of select="./text()"/>
                            </datacite:creatorName>

<!-- MHV Sept.2020 Je vais insérer ici nos ORCID ID du (seul) auteur de la these,
  donc on sait que le ID se rattache à lui/elle ; je ne touche pas aux valeurs d'autorités (que l'on utilise pas presentement)-->

<!--
			<xsl:if test="$estUneTME = 'true'">
				<xsl:for-each select="$UdeM/doc:element[@name='ORCIDAuteurThese']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]">
				<datacite:nameIdentifier nAmeIdentifierScheme="ORCID" schemeURI="http://orcid.org">
				<xsl:value-of select="."/>
				</datacite:nameIdentifier>
				</xsl:for-each>
			</xsl:if>
-->			
			
<!-- A MHV Octobre 2023. On (tech DTDM) veut pouvoir mettre le ORCID dans le cas d'un auteur SEUL; donc pas uniquement les theses
							je fais le test pour s'assurer un seul ORCID et un seul auteur. On n'utilise pas "creator" alors je ne le considère pas. À revoir si ca en venait à changer
							-->			

                        	<xsl:if test="(
                        		(count($UdeM/doc:element[@name='ORCIDAuteurThese']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]) = 1)
                        		and
                        		(count($dc/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]) = 1)
                        		)">
                        		
                        			<datacite:nameIdentifier nameIdentifierScheme="ORCID" schemeURI="http://orcid.org">
                        				<xsl:value-of select="$UdeM/doc:element[@name='ORCIDAuteurThese'][1]/doc:element/doc:field[@name = 'value' and descendant::text()[normalize-space()]]" />
                        			</datacite:nameIdentifier>

                        	</xsl:if>
                        	

                        </datacite:creator>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
        </datacite:creators>
    </xsl:template>

   <!--  -->
   <!-- Auxiliary templates -->
   <!--  -->

    <!--
        this template will verify if a field name with "authority"
        is present and if it is not empty
        if it occurs, than we are in a presence of a related entity
     -->
    <xsl:template name="isAuthorityValue">
        <xsl:param name="element"/>
        <xsl:variable name="sibling1" select="$element/following-sibling::*[1]"/>
        <!-- if next sibling is authority and is not empty -->
        <xsl:choose>
            <xsl:when test="$sibling1[@name='authority' and (text()!='')]">
                <xsl:value-of select="true()"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="false()"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--
        this template will try to look for all "additional metadata"
        This will retrieve something like:
        <element name="10f8e9ba-6d4f-4550-beae-53e5d07c66fc">
           <field name="dc.contributor.author.none">Doe, John</field>
           <field name="person.identifier.orcid">3f685bbd-07d9-403e-9de2-b8f0fabe27a7</field>
        </element>
     -->
    <xsl:template name="buildAuthorityNode">
        <xsl:param name="element"/>
        <!-- authority? -->
        <xsl:variable name="sibling1" select="$element/following-sibling::*[1]"/>
        <!-- confidence? -->
        <xsl:variable name="sibling2" select="$element/following-sibling::*[2]"/>
        <!-- if next sibling is authority and is not empty -->
        <xsl:if test="$sibling1[@name='authority' and (text()!='')]">
            <xsl:variable name="relation_id" select="$sibling1[1]/text()"/>
            <xsl:element name="element" namespace="http://www.lyncode.com/xoai">
                <xsl:attribute name="name">
                    <xsl:value-of select="$relation_id"/>
                </xsl:attribute>
                <!-- search for all virtual relations elements in XML -->
                <xsl:for-each select="//doc:field[text()=$relation_id]/preceding-sibling::*[1]">
                    <xsl:element name="field" namespace="http://www.lyncode.com/xoai">
                        <xsl:attribute name="name">
                        <xsl:call-template name="buildAuthorityFieldName">
                        <xsl:with-param name="element" select="."/>
                          </xsl:call-template>
                        </xsl:attribute>
                        <!-- field value -->
                        <xsl:value-of select="./text()"/>
                    </xsl:element>
                </xsl:for-each>
            </xsl:element>
        </xsl:if>
    </xsl:template>

   <!--
        This template will recursively create the field name based on parent node names
        to be something like this:
        person.familyName.*
    -->
    <xsl:template name="buildAuthorityFieldName">
        <xsl:param name="element"/>
        <xsl:choose>
            <xsl:when test="$element/..">
                <xsl:call-template name="buildAuthorityFieldName">
                    <xsl:with-param name="element" select="$element/.."/>
                </xsl:call-template>
                <!-- if parent isn't an element then don't include '.' -->
                <xsl:if test="local-name($element/../..) = 'element'">
                    <xsl:text>.</xsl:text>
                </xsl:if>
                <xsl:value-of select="$element/../@name"/>
            </xsl:when>
            <xsl:otherwise/>
        </xsl:choose>
    </xsl:template>

    <!-- datacite:creator -->
    <xsl:template match="doc:element" mode="authority_creator">
        <datacite:creator>
            <datacite:creatorName>
                <xsl:value-of select="doc:field[starts-with(@name,'dc.contributor.author')]"/>
            </datacite:creatorName>
            <xsl:choose>
            <xsl:when test="doc:field[starts-with(@name,'person.identifier.orcid')]">
		        <datacite:nameIdentifier nameIdentifierScheme="ORCID" schemeURI="http://orcid.org">
		        	<xsl:value-of select="doc:field[starts-with(@name,'person.identifier.orcid')]"/>
		        </datacite:nameIdentifier>
            </xsl:when>
            <xsl:otherwise/>
        	</xsl:choose>
        </datacite:creator>
    </xsl:template>

<xsl:template name="format">
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
								<xsl:for-each
									select="$bundles/doc:element[@name = 'bundle']/
									doc:field[@name = 'name' and text() = 'ORIGINAL']/../doc:element[@name = 'bitstreams']/
									doc:element[@name = 'bitstream'][not(doc:field[@name = 'format']=preceding-sibling::
										doc:element[@name = 'bitstream']/doc:field[@name = 'format']
										)]">
									<dc:format>
										<xsl:value-of
											select="./doc:field[@name = 'format']"/>
									</dc:format>
								</xsl:for-each>
			</xsl:when>
			<xsl:otherwise />
		</xsl:choose>
</xsl:template>


<xsl:template name="AjouterMoisAUneDate">
    <xsl:param name="dateTime"/>
    <xsl:param name="months-to-add"/>
    <!-- extract components -->
    <xsl:variable name="year" select="substring($dateTime, 1, 4)"/>
    <xsl:variable name="month" select="substring($dateTime, 6, 2)"/>
<!--    <xsl:variable name="day" select="substring($dateTime, 9, 2)"/> -->
    <xsl:variable name="time" select="substring-after($dateTime, 'T')"/>

<xsl:variable name="day">
<xsl:choose>
			<xsl:when test="string(number(substring($dateTime, 9, 2))) != 'NaN'">
				<xsl:value-of select="substring($dateTime, 9, 2)"/>
			</xsl:when>
			<xsl:otherwise><!-- si il n'y a pas de jour de spécifié je pousse "01" -->
				<xsl:value-of select="01"/>
			</xsl:otherwise>
</xsl:choose>
</xsl:variable>


    <!-- calculate target year and month (using Knuth's corrected mod) -->
    <xsl:variable name="m11" select="$month + $months-to-add - 1"/>
    <xsl:variable name="y" select="$year + floor($m11 div 12)"/>
    <xsl:variable name="m" select="$m11 - 12 * floor($m11 div 12) + 1"/>
    <!-- calculate target day (clipped to last day of target month, excess days do not overflow) -->
    <xsl:variable name="cal" select="'312831303130313130313031'"/>
    <xsl:variable name="leap" select="not($y mod 4) and $y mod 100 or not($y mod 400)"/>
    <xsl:variable name="month-length" select="substring($cal, 2 * ($m - 1) + 1, 2) + ($m = 2 and $leap)" />
    <xsl:variable name="d">
        <xsl:choose>
            <xsl:when test="$day > $month-length">
                <xsl:value-of select="$month-length"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$day"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <!-- output -->
    <xsl:value-of select="$y"/>-<xsl:value-of select="format-number($m, '00')"/>-<xsl:value-of select="format-number($d, '00')"/>
<!-- MHV Je ne veux pas outputer l'heure -->
<!-- <xsl:text>T</xsl:text> -->
<!-- <xsl:value-of select="$time"/> -->
</xsl:template>

</xsl:stylesheet>