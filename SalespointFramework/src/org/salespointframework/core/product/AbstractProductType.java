package org.salespointframework.core.product;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EmbeddedId;
import javax.persistence.MappedSuperclass;

import org.salespointframework.core.money.Money;
import org.salespointframework.core.product.features.ProductFeatureType;
import org.salespointframework.util.Objects;
import org.salespointframework.util.SalespointIterable;

@MappedSuperclass
public class AbstractProductType implements ProductType {
	
	@EmbeddedId
	protected ProductIdentifier productIdentifier;
	protected String name;
	
	protected Money price;
	
	//TODO Map?
	protected Set<ProductFeatureType> featureTypes = new HashSet<ProductFeatureType>();
	@Deprecated
	protected AbstractProductType() {
		
	}
	
	public AbstractProductType(String name, Money price) {
		//this.productIdentifier = Objects.requireNonNull(productIdentifier, "productIdentifier");
		this.name = Objects.requireNonNull(name, "name");
		this.price = Objects.requireNonNull(price, "price");
		this.productIdentifier = new ProductIdentifier();
	}
	
	@Override
	public boolean equals(Object other) {
		if(other == null) return false;
		if(other == this) return true;
		if(!(other instanceof ProductType)) return false;
		return this.equals((ProductType)other);
	}
	
	public boolean equals(ProductType other) {
		if(other == null) return false;
		if(other == this) return true;
		return this.productIdentifier.equals(other.getProductIdentifier());
	}
	
	@Override
	public int hashCode() {
		return productIdentifier.hashCode();
	}
	
	@Override	
	public Money getPrice() {
		return price;
	}

	@Override
	public Iterable<ProductFeatureType> getProductFeatureTypes() {
		return SalespointIterable.from(featureTypes);
	}
	
	@Override
	public ProductIdentifier getProductIdentifier() {
		return productIdentifier;
	}

	@Override
	public void addProductFeatureType (ProductFeatureType pf){
		if (pf!=null)
		featureTypes.add(pf);
		
	}
	
	@Override
	public void removeProductFeatureType (ProductFeatureType pf){
		if (pf!=null)
		featureTypes.remove(pf);
	}
	
	@Override
	public String getName() {
		return name;
	}
	
}
