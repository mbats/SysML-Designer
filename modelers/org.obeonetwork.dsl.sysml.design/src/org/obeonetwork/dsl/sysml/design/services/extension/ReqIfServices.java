package org.obeonetwork.dsl.sysml.design.services.extension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.rmf.reqif10.ReqIF;
import org.eclipse.rmf.reqif10.SpecHierarchy;
import org.eclipse.rmf.reqif10.Specification;
import org.eclipse.rmf.serialization.ReqIFResourceImpl;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.UMLFactory;
import org.obeonetwork.dsl.sysml.design.services.SysMLServices;

import reqifext.Relation;
import reqifext.ReqIfExtension;
import reqifext.ReqifextFactory;
import reqifext.ReqifextPackage;
import reqifext.Satisfy;
import reqifext.Verify;
import reqifext.util.ReqifextResourceFactoryImpl;
import fr.obeo.dsl.viewpoint.business.api.session.Session;
import fr.obeo.dsl.viewpoint.business.api.session.SessionManager;

public class ReqIfServices {
	public List<SpecHierarchy> getReqIfSpecifications(final Model model) {
		final Session session = SessionManager.INSTANCE.getSession(model);
		List<SpecHierarchy> result = new ArrayList<SpecHierarchy>();
		for (Resource resource : session.getSemanticResources()) {
			if (resource instanceof ReqIFResourceImpl) {
				EObject root = resource.getContents().get(0);
				for (Specification specification : ((ReqIF)root).getCoreContent().getSpecifications()) {
					result.addAll(specification.getChildren());
				}
			}
		}
		return result;
	}

	public void addReqIfSpecification(final Model model) {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			public void run() {
				ResourceSelectionDialog dialog = new ResourceSelectionDialog(PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getShell(), ResourcesPlugin.getWorkspace().getRoot(),
						"Select a Requirement Specification:");
				dialog.setTitle("Requirement Specification Selection");
				dialog.open();
				Object[] results = dialog.getResult();
				if (results == null || results.length == 0) {
					return;
				}
				final org.eclipse.core.internal.resources.File result = (org.eclipse.core.internal.resources.File)results[0];

				// Create a resource set to hold the resources.
				ResourceSet resourceSet = new ResourceSetImpl();

				// Register the appropriate resource factory to handle all file extensions.
				resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
						.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new ReqifextResourceFactoryImpl());

				// Register the package to ensure it is available during loading.
				resourceSet.getPackageRegistry().put(ReqifextPackage.eNS_URI, ReqifextPackage.eINSTANCE);

				final Session session = SessionManager.INSTANCE.getSession(model);
				URI uri = ((Resource)session.getSemanticResources().toArray()[0]).getURI();
				String uripath = "platform:" + uri.path().substring(0, uri.path().indexOf(uri.lastSegment()))
						+ "requirements.reqifext";

				Resource resource = resourceSet.createResource(URI.createURI(uripath));

				ReqIfExtension extension = ReqifextFactory.eINSTANCE.createReqIfExtension();
				resource.getContents().add(extension);
				try {
					resource.save(null);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// Add resource to session semantic resources
				addSemanticResources(
						URI.createURI("file://" + result.getLocation().toFile().getAbsolutePath()), session);
				addSemanticResources(URI.createURI(uripath), session);
			}
		});
	}

	public void removeReqIfSpecification(final EObject object) {
		// Remove resource to session semantic resources
		removeSemanticResources(object);
	}

	public void assignRequirement(EObject element) {
		System.out.println("Assign requirement");
	}

	public void satisfyRequirement(org.eclipse.uml2.uml.Class block, SpecHierarchy requirement) {
		System.out.println("Satisfy requirement");
		// Create ReqIf
		// org.eclipse.uml2.uml.Class reqIf = getReqIf(requirement.getIdentifier());

		// Create ReqIfRelated
		Class reqIfRelated = UMLFactory.eINSTANCE.createClass();
		reqIfRelated.setName(block.getName() + "_satisfy_" + requirement.getIdentifier());
		SysMLServices service = new SysMLServices();
		service.createAssociatedStereotype(reqIfRelated, "SysML::Requirements", "ReqIfRelated");
	}

	public List<SpecHierarchy> getRequirements(org.eclipse.uml2.uml.Class element) {
		List<SpecHierarchy> result = new ArrayList<SpecHierarchy>();
		List<SpecHierarchy> specifications = getReqIfSpecifications((Model)element.eContainer());
		for (SpecHierarchy specification : specifications) {
			specification.getChildren();
		}

		return result;
	}

	/**
	 * Add the semantic resource.
	 * 
	 * @param uris
	 *            the uris to load
	 * @return the resource added to the session.
	 */
	private Collection<Resource> addSemanticResources(final URI uri, final Session session) {
		final Collection<Resource> resources = new ArrayList<Resource>();
		final EObject root = session.getSemanticResources().iterator().next().getContents().get(0);
		final TransactionalEditingDomain domain = TransactionUtil.getEditingDomain(root);

		final Resource res = domain.getResourceSet().getResource(uri, true);

		if (res != null && !res.getContents().isEmpty())
			resources.add(res);

		final Command cmd = new RecordingCommand(domain) {
			@Override
			protected void doExecute() {
				for (final Resource res : resources) {
					session.addSemanticResource(res, true);
				}
			}
		};
		domain.getCommandStack().execute(cmd);
		return resources;
	}

	private Collection<Resource> removeSemanticResources(final EObject object) {
		final Session session = SessionManager.INSTANCE.getSession(object);
		final Collection<Resource> resources = new ArrayList<Resource>();
		final EObject root = session.getSemanticResources().iterator().next().getContents().get(0);
		final TransactionalEditingDomain domain = TransactionUtil.getEditingDomain(root);

		final Resource res = object.eResource();

		if (res != null && !res.getContents().isEmpty())
			resources.add(res);

		final Command cmd = new RecordingCommand(domain) {
			@Override
			protected void doExecute() {
				for (final Resource res : resources) {
					session.removeSemanticResource(res, true);
				}
			}
		};
		domain.getCommandStack().execute(cmd);
		return resources;
	}

	private List<SpecHierarchy> getRequirements(SpecHierarchy element) {
		List<SpecHierarchy> requirements = new ArrayList<SpecHierarchy>();
		if ("Requirement Type".equals(element.getObject().getType().getLongName())) {
			requirements.add(element);
		}
		if (element.getChildren() != null) {
			for (SpecHierarchy specHierarchy : element.getChildren()) {
				requirements.addAll(getRequirements(specHierarchy));
			}
		}

		return requirements;
	}

	public List<SpecHierarchy> getRequirements(final Model model) {
		List<SpecHierarchy> result = new ArrayList<SpecHierarchy>();
		List<SpecHierarchy> specifications = getReqIfSpecifications(model);
		for (SpecHierarchy specification : specifications) {
			result.addAll(getRequirements(specification));
		}
		return result;
	}

	public void createSatisfy(SpecHierarchy requirement, Class block) {
		// Create satisfy relation
		final Session session = SessionManager.INSTANCE.getSession(block);
		for (Resource resource : session.getSemanticResources()) {
			if (resource.getContents() != null && resource.getContents().get(0) instanceof ReqIfExtension) {
				ReqIfExtension root = (ReqIfExtension)resource.getContents().get(0);
				Satisfy satisfy = ReqifextFactory.eINSTANCE.createSatisfy();
				satisfy.setName(requirement.getIdentifier() + "_" + block.getQualifiedName());
				satisfy.setRequirement(requirement);
				satisfy.setSysmlElement(block);
				root.getRelations().add(satisfy);
			}
		}
	}

	public void createVerify(SpecHierarchy requirement, Class block) {
		// Create verify relation
		final Session session = SessionManager.INSTANCE.getSession(block);
		for (Resource resource : session.getSemanticResources()) {
			if (resource.getContents() != null && resource.getContents().get(0) instanceof ReqIfExtension) {
				ReqIfExtension root = (ReqIfExtension)resource.getContents().get(0);
				Verify verify = ReqifextFactory.eINSTANCE.createVerify();
				verify.setName(requirement.getIdentifier() + "_" + block.getQualifiedName());
				verify.setRequirement(requirement);
				verify.setSysmlElement(block);
				root.getRelations().add(verify);
			}
		}
	}

	public List<Satisfy> getSatisfyRelations(EObject model) {
		List<Satisfy> results = new ArrayList<Satisfy>();
		final Session session = SessionManager.INSTANCE.getSession(model);
		for (Resource resource : session.getSemanticResources()) {
			if (resource.getContents() != null && resource.getContents().get(0) instanceof ReqIfExtension) {
				ReqIfExtension root = (ReqIfExtension)resource.getContents().get(0);
				for (Relation relation : root.getRelations()) {
					if (relation instanceof Satisfy)
						results.add((Satisfy)relation);
				}
			}
		}
		return results;
	}
	
	public List<Verify> getVerifyRelations(EObject model) {
		List<Verify> results = new ArrayList<Verify>();
		final Session session = SessionManager.INSTANCE.getSession(model);
		for (Resource resource : session.getSemanticResources()) {
			if (resource.getContents() != null && resource.getContents().get(0) instanceof ReqIfExtension) {
				ReqIfExtension root = (ReqIfExtension)resource.getContents().get(0);
				for (Relation relation : root.getRelations()) {
					if (relation instanceof Verify)
						results.add((Verify)relation);
				}
			}
		}
		return results;
	}
	
	public ReqIfExtension getRelationsContainer(EObject model) {
		final Session session = SessionManager.INSTANCE.getSession(model);
		for (Resource resource : session.getSemanticResources()) {
			if (resource.getContents() != null && resource.getContents().get(0) instanceof ReqIfExtension) {
				return (ReqIfExtension)resource.getContents().get(0);
			}
		}
		return null;
	}
}
